import org.jahia.utils.i18n.JahiaResourceBundle

if(binding.variables.containsKey("param1")){
    try {
        def bundleName = binding.variables.containsKey("param2") ? param2 : null;
        print JahiaResourceBundle.getString(bundleName, param1, renderContext.getMainResourceLocale(), renderContext.getSite().getTemplatePackageName());
    } catch(java.util.MissingResourceException e) {
        print org.apache.commons.lang.StringEscapeUtils.escapeXml(param1);
    }
}else{
    print "<p>This macro require one or two parameter like : <br />" +
            "## resourceBundle(parameter) ##  or ## resourceBundle(parameter1, parameter2) ##</p>";
}