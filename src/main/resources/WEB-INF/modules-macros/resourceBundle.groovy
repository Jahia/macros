import org.apache.commons.lang.StringEscapeUtils
import org.jahia.utils.i18n.Messages

if(binding.variables.containsKey("param1")){
    try {
        def bundleName = binding.variables.containsKey("param2") ? param2 : null;
        print StringEscapeUtils.escapeHtml(Messages.get(bundleName, renderContext.getSite().getTemplatePackage(), param1, renderContext.getMainResourceLocale()));
    } catch(java.util.MissingResourceException e) {
        print StringEscapeUtils.escapeXml(param1);
    }
}else{
    print "<p>This macro require one or two parameter like : <br />" +
            "## resourceBundle(parameter) ##  or ## resourceBundle(parameter1, parameter2) ##</p>";
}