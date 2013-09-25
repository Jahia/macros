/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2013 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */

package org.jahia.modules.macros.filter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.script.*;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLGenerator;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.jahia.services.templates.JahiaTemplateManagerService.TemplatePackageRedeployedEvent;
import org.jahia.utils.FileUtils;
import org.jahia.utils.Patterns;
import org.jahia.utils.ScriptEngineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;

/**
 * Render filter that searches for known macros in the generated HTML output and evaluates them.
 *
 * @author rincevent
 * @since JAHIA 6.5
 * Created : 21/12/10
 */
public class MacrosFilter extends AbstractFilter implements InitializingBean, ApplicationListener<TemplatePackageRedeployedEvent> {

    private transient static Logger logger = LoggerFactory.getLogger(MacrosFilter.class);

    private String[] macroLookupPath;
    private Pattern macrosPattern;
    private Map<String, String[]> scriptCache;
    private ScriptEngineUtils scriptEngineUtils;
    private boolean replaceByErrorMessageOnMissingMacros = true;
    public void afterPropertiesSet() throws Exception {
        scriptCache = new LinkedHashMap<String, String[]>();
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
                try {
                    // execute macro
                    ScriptEngine scriptEngine = scriptEngineUtils.scriptEngine(macro[1]);
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
        bindings.put( "url", new URLGenerator(renderContext, resource));
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

        packages.add(ServicesRegistry.getInstance().getJahiaTemplateManagerService().getTemplatePackageByFileName("macros"));

        for (String s : m) {
            JahiaTemplatesPackage pack = ServicesRegistry.getInstance().getJahiaTemplateManagerService().getTemplatePackageByFileName(s);
            packages.add(pack);
            packages.addAll(pack.getDependencies());
        }

        try {
            for (JahiaTemplatesPackage aPackage : packages) {
                for (String path : macroLookupPath) {
                    org.springframework.core.io.Resource[] resources = aPackage.getResources(path);
                    for (org.springframework.core.io.Resource resource : resources) {
                        if (resource.getFilename().startsWith(macroName)) {
                            macro = new String[] { FileUtils.getContent(resource),
                                    FilenameUtils.getExtension(resource.getFilename()) };

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

        logger.info("No macro script found for macro \"{}\" using lookup \"{}\".",
                macroName, "");
        if(!replaceByErrorMessageOnMissingMacros) {
            scriptCache.put(macroName, macro);
        }
        return null;
    }

    public void onApplicationEvent(TemplatePackageRedeployedEvent event) {
        scriptCache.clear();
    }

    public void setMacroLookupPath(String macroLookupPath) {
        this.macroLookupPath = macroLookupPath.split(",");
    }

    public void setMacrosRegexp(String macrosRegexp) {
        this.macrosPattern = Pattern.compile(macrosRegexp);
    }

    public void setScriptEngineUtils(ScriptEngineUtils scriptEngineUtils) {
        this.scriptEngineUtils = scriptEngineUtils;
    }

    public void setReplaceByErrorMessageOnMissingMacros(boolean replaceByErrorMessageOnMissingMacros) {
        this.replaceByErrorMessageOnMissingMacros = replaceByErrorMessageOnMissingMacros;
    }
}
