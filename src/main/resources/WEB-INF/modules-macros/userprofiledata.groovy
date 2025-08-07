if (binding.variables.containsKey("param1")) {
    if(binding.variables.containsKey("param2")){
        pathUserNode = currentUser.getLocalPath() + "/" + param1;
        try {
            print renderContext.getSite().getSession().getNode(pathUserNode).getProperty(param2).getString();
        } catch(Exception e) {
            print org.apache.commons.lang.StringEscapeUtils.escapeXml("Unknown parameter: \"${e.getMessage()}\"!");
        }
    } else if (currentUser.getUserProperty(param1) == null) {
        print org.apache.commons.lang.StringEscapeUtils.escapeXml("Unknown parameter: \"${param1}\"!")
    } else {
        print currentUser.getUserProperty(param1).getValue();
    }
} else {
    print """
        <p>This macro requires one or two parameters, like:<br />
        ## userprofiledata(parameter) ## or ## userprofiledata(parameter1, parameter2) ##</p>
    """
}
