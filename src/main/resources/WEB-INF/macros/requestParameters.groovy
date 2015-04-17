def printEncodedString(parameter){
    if(parameter.contains("%25")){
        def values = parameter.split('%')
        for(param in values[0..<values.size()-1]){
            print URLEncoder.encode(param, "UTF-8")
            print "%"
        }
        print URLEncoder.encode(values[values.size()-1], "UTF-8")
    }
    else{
        print URLEncoder.encode(parameter, "UTF-8")
    }
}

if (renderContext.getRequest().getQueryString() != null) {
    print "?"
    def paramsList = renderContext.getRequest().getQueryString().split('&')
    for(param in paramsList[0..<paramsList.size()-1]){
        def (paramName, value) = param.tokenize( '=' )
        printEncodedString(paramName)
        print "="
        printEncodedString(value)
        print "&"
    }
    def (paramName, value) = paramsList[paramsList.size()-1].tokenize( '=' )
    printEncodedString(paramName)
    print "="
    printEncodedString(value)
}