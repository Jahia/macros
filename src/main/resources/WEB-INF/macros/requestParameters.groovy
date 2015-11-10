if (renderContext.getRequest().getQueryString() != null) {
    print "?" + renderContext.getRequest().getQueryString()
} else if (renderContext.getRequest().getAttribute("javax.servlet.forward.query_string") != null) {
    print "?" + renderContext.getRequest().getAttribute("javax.servlet.forward.query_string")
}