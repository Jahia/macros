package org.jahia.modules.macros;

import org.apache.commons.io.IOUtils;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.utils.i18n.JahiaResourceBundle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Runs the module macros the way {@code MacrosFilter} does: through the JSR-223 Groovy engine, with the
 * same bindings, and reading what the script printed.
 */
public class ModuleMacrosOutputTest {

    private RenderContext renderContext;
    private JCRSiteNode site;
    private JCRNodeWrapper currentNode;
    private HttpServletRequest request;
    private MockedStatic<JahiaResourceBundle> resourceBundle;

    @Before
    public void setUp() {
        renderContext = mock(RenderContext.class);
        site = mock(JCRSiteNode.class);
        currentNode = mock(JCRNodeWrapper.class);
        request = mock(HttpServletRequest.class);
        when(renderContext.getSite()).thenReturn(site);
        when(renderContext.getRequest()).thenReturn(request);
        when(renderContext.getMainResourceLocale()).thenReturn(Locale.ENGLISH);
        when(site.getTemplatePackageName()).thenReturn("templates");
        resourceBundle = mockStatic(JahiaResourceBundle.class);
    }

    @After
    public void tearDown() {
        resourceBundle.close();
    }

    @Test
    public void aBundleValueWithoutMarkupIsPrintedUnchanged() throws Exception {
        givenBundleValue("label.yes", "Yes");

        assertEquals("Yes", run("resourceBundle", "label.yes"));
    }

    @Test
    public void aBundleValueIsPrintedAsHtmlText() throws Exception {
        givenBundleValue("label.quoted", "Say \"hello\" <b>now</b> & later");

        assertEquals("Say &quot;hello&quot; &lt;b&gt;now&lt;/b&gt; &amp; later",
                run("resourceBundle", "label.quoted"));
    }

    @Test
    public void aMissingBundleKeyIsPrintedAsHtmlText() throws Exception {
        resourceBundle.when(() -> JahiaResourceBundle.getString(isNull(), eq("a&b"), any(Locale.class), eq("templates")))
                .thenThrow(new MissingResourceException("missing", "bundle", "a&b"));

        assertEquals("a&amp;b", run("resourceBundle", "a&b"));
    }

    @Test
    public void anAuthorNameIsPrintedAsHtmlText() throws Exception {
        when(currentNode.getCreationUser()).thenReturn("jane <b>doe</b>");

        assertEquals("jane &lt;b&gt;doe&lt;/b&gt;", run("authorname", null));
    }

    @Test
    public void anAuthorNameWithoutMarkupIsPrintedUnchanged() throws Exception {
        when(currentNode.getCreationUser()).thenReturn("jane");

        assertEquals("jane", run("authorname", null));
    }

    @Test
    public void theHomePageLinkIsPrintedAsAnAttributeValue() throws Exception {
        JCRNodeWrapper home = mock(JCRNodeWrapper.class);
        when(site.getHome()).thenReturn(home);
        when(home.getAbsoluteUrl(request)).thenReturn("http://example.org/home.html?a=1&b=\"2\"");

        assertEquals("http://example.org/home.html?a=1&amp;b=&quot;2&quot;", run("linktohomepage", null));
    }

    @Test
    public void theParentPageLinkIsPrintedAsAnAttributeValue() throws Exception {
        JCRNodeWrapper parent = mock(JCRNodeWrapper.class);
        when(parent.getAbsoluteUrl(request)).thenReturn("http://example.org/parent.html?a=1&b=\"2\"");
        try (MockedStatic<JCRContentUtils> contentUtils = mockStatic(JCRContentUtils.class)) {
            contentUtils.when(() -> JCRContentUtils.getParentOfType(currentNode, "jnt:page")).thenReturn(parent);

            assertEquals("http://example.org/parent.html?a=1&amp;b=&quot;2&quot;", run("linktoparent", null));
        }
    }

    private void givenBundleValue(String key, String value) {
        resourceBundle.when(() -> JahiaResourceBundle.getString(isNull(), eq(key), any(Locale.class), eq("templates")))
                .thenReturn(value);
    }

    private String run(String macro, String param1) throws Exception {
        String script;
        try (InputStream in = getClass().getResourceAsStream("/WEB-INF/modules-macros/" + macro + ".groovy")) {
            script = IOUtils.toString(in, StandardCharsets.UTF_8);
        }
        ScriptEngine engine = new ScriptEngineManager().getEngineByExtension("groovy");
        SimpleBindings bindings = new SimpleBindings();
        bindings.put("renderContext", renderContext);
        bindings.put("currentNode", currentNode);
        if (param1 != null) {
            bindings.put("param1", param1);
        }
        ScriptContext context = new SimpleScriptContext();
        context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        StringWriter out = new StringWriter();
        context.setWriter(out);
        engine.eval(script, context);
        return out.toString().trim();
    }
}
