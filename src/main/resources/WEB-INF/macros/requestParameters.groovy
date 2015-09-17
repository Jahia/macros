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
        if(param.contains("=")) {
            printEncodedString(param.substring(0, param.indexOf("=")))
            print "="
            printEncodedString(param.substring(param.indexOf("=") + 1))
            print "&"
        } else {
            printEncodedString(param)
            print "&"
        }
    }
    def lastParam = paramsList[paramsList.size()-1];
    if(lastParam.contains("=")) {
        printEncodedString(lastParam.substring(0, lastParam.indexOf("=")))
        print "="
        printEncodedString(lastParam.substring(lastParam.indexOf("=") + 1))
    } else {
        print(paramsList[paramsList.size()-1])
    }
}