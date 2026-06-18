/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.macros.filter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.osgi.BundleUtils;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLGenerator;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.jahia.utils.FileUtils;
import org.jahia.utils.Patterns;
import org.jahia.utils.ScriptEngineUtils;
import org.osgi.framework.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.script.*;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Render filter that searches for known macros in the generated HTML output and evaluates them.
 *
 * @author rincevent
 * @since JAHIA 6.5
 */
public abstract class MacrosFilter extends AbstractFilter {

    private static final Logger logger = LoggerFactory.getLogger(MacrosFilter.class);

    private SynchronousBundleListener bundleListener;

    private String[] macroLookupPath;
    private Pattern macrosPattern;
    private Map<String, String[]> scriptCache;
    private boolean replaceByErrorMessageOnMissingMacros = true;

    public MacrosFilter() {
        this.scriptCache = new LinkedHashMap<>();
    }

    @Override
    public String execute(String previousOut, RenderContext renderContext, Resource resource, RenderChain chain)
            throws Exception {
        if (StringUtils.isEmpty(previousOut)) {
            return previousOut;
        }
        long timer = System.currentTimeMillis();
        boolean evaluated = false;

        Matcher matcher = macrosPattern.matcher(previousOut);
        while (matcher.find()) {
            evaluated = true;
            String macroName = matcher.group(1);
            if (StringUtils.isEmpty(macroName)) {
                continue;
            }
            String[] macro = getMacro(macroName, renderContext);
            if (macro != null) {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                JahiaTemplatesPackage module = ServicesRegistry.getInstance().getJahiaTemplateManagerService().getTemplatePackageById(macro[2]);
                Thread.currentThread().setContextClassLoader(module.getChainedClassLoader());
                try {
                    // execute macro
                    ScriptEngine scriptEngine = ScriptEngineUtils.getInstance().scriptEngine(macro[1]);
                    ScriptContext scriptContext = new SimpleScriptContext();
                    scriptContext.setBindings(getBindings(renderContext, resource, scriptContext, matcher), ScriptContext.ENGINE_SCOPE);
                    scriptContext.setBindings(scriptEngine.getContext().getBindings(ScriptContext.GLOBAL_SCOPE), ScriptContext.GLOBAL_SCOPE);
                    scriptContext.setWriter(new StringWriter());
                    scriptContext.setErrorWriter(new StringWriter());
                    scriptEngine.eval(macro[0],scriptContext);
                    String scriptResult = scriptContext.getWriter().toString().trim();
                    previousOut = StringUtils.replace(previousOut, matcher.group(), scriptResult);
                } catch (ScriptException e) {
                    logger.warn("Error during execution of macro "+macroName+" with message "+ e.getMessage(), e);
                    previousOut = matcher.replaceFirst(macroName);
                } finally {
                    Thread.currentThread().setContextClassLoader(tccl);
                }
                matcher = macrosPattern.matcher(previousOut);
            } else if(replaceByErrorMessageOnMissingMacros) {
                previousOut = matcher.replaceFirst("macro " + macroName + " not found");
                logger.warn("Unknown macro '{}'", macroName);
                matcher = macrosPattern.matcher(previousOut);
            }
        }

        if (evaluated && logger.isDebugEnabled()) {
            logger.debug("Evaluation of macros took {} ms", (System.currentTimeMillis() - timer));
        }
        return previousOut;
    }

    private Bindings getBindings(RenderContext renderContext, Resource resource, ScriptContext scriptContext, Matcher matcher) {
        Bindings bindings = new SimpleBindings();
        bindings.put("currentUser", renderContext.getUser());
        bindings.put("currentNode", resource.getNode());
        bindings.put("currentResource", resource);
        bindings.put("renderContext", renderContext);

        // avoid re-creating a URLGenerator if we don't need to
        URLGenerator generator = renderContext.getURLGenerator();
        if(!generator.uses(resource)) {
            generator = new URLGenerator(renderContext, resource);
        }
        bindings.put( "url", generator);

        String group = matcher.group(3);
        if(group!=null) {
            int i = 1;
            for (String s : Patterns.COMMA.split(group)) {
                bindings.put("param"+(i++), s);
            }
        }
        try {
            bindings.put("currentAliasUser", renderContext.getMainResource().getNode().getSession().getAliasedUser());
        } catch (RepositoryException e) {
            logger.error(e.getMessage(), e);
        }
        return bindings;
    }

    protected String[] getMacro(String macroName, RenderContext renderContext) {
        String[] macro = scriptCache.get(macroName);

        if (macro != null || (!replaceByErrorMessageOnMissingMacros && scriptCache.containsKey(macroName))) {
            return macro;
        }

        List<String> m = renderContext.getSite().getInstalledModules();
        Set<JahiaTemplatesPackage> packages = new LinkedHashSet<JahiaTemplatesPackage>();

        packages.add(ServicesRegistry.getInstance().getJahiaTemplateManagerService().getTemplatePackageById("macros"));

        for (String s : m) {
            JahiaTemplatesPackage pack = ServicesRegistry.getInstance().getJahiaTemplateManagerService().getTemplatePackageById(s);
            // pack can be null if the module has been stopped
            if (pack != null) {
                packages.add(pack);
                packages.addAll(pack.getDependencies());
            }
        }

        try {
            for (JahiaTemplatesPackage aPackage : packages) {
                for (String path : macroLookupPath) {
                    org.springframework.core.io.Resource[] resources = aPackage.getResources(path);
                    for (org.springframework.core.io.Resource resource : resources) {
                        if (resource.getFilename().startsWith(macroName)) {
                            macro = new String[] { FileUtils.getContent(resource),
                                    FilenameUtils.getExtension(resource.getFilename()), aPackage.getId() };

                            scriptCache.put(macroName, macro);

                            if (logger.isTraceEnabled()) {
                                logger.trace("Script of type {}, content:\n{}", macro[1], macro[0]);
                            }
                            return macro;
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Cannot read files",e);
        }

        if(!replaceByErrorMessageOnMissingMacros) {
            scriptCache.put(macroName, null);
        }
        return null;
    }

    @Activate
    public void start(BundleContext bundleContext) {
        bundleListener = bundleEvent -> {
            Bundle bundle = bundleEvent.getBundle();
            if (bundle == null) {
                return;
            }

            if (!BundleUtils.isJahiaModuleBundle(bundle)) {
                return;
            }

            int bundleEventType = bundleEvent.getType();
            if (bundleEventType == BundleEvent.STARTED || bundleEventType == BundleEvent.STOPPED) {
                scriptCache.clear();
            }
        };

        bundleContext.addBundleListener(bundleListener);
    }

    @Deactivate
    public void stop(BundleContext bundleContext) {
        bundleContext.removeBundleListener(bundleListener);
    }

    public void setMacroLookupPath(String macroLookupPath) {
        this.macroLookupPath = macroLookupPath.split(",");
    }

    public void setMacrosRegexp(String macrosRegexp) {
        this.macrosPattern = Pattern.compile(macrosRegexp);
    }

    public void setReplaceByErrorMessageOnMissingMacros(boolean replaceByErrorMessageOnMissingMacros) {
        this.replaceByErrorMessageOnMissingMacros = replaceByErrorMessageOnMissingMacros;
    }
}
