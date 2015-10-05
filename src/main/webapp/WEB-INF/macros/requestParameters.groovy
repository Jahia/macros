def queryString = renderContext.getRequest().getQueryString()

if (queryString != null) {
    if ("true".equals(renderContext.getRequest().getParameter("encoded_querystring"))) {
        print "?" + queryString;
    } else {
        def paramsList = queryString.split('&')
        def encoded = false;
        paramsList.eachWithIndex { param, index ->
            print index < 1 ? "?" : "&"
            def (paramName, value) = param.tokenize('=')
            if (paramName != null) {
                print URLEncoder.encode(paramName, "UTF-8")
                encoded = true;
                if (value != null) {
                    print "="
                    print URLEncoder.encode(value, "UTF-8")
                }
            }
        }
        if (encoded) {
            print "&encoded_querystring=true"
        }
    }
}