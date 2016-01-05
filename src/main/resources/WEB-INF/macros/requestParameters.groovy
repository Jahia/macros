if (renderContext.getRequest().getQueryString() != null) {
    print "?" + org.apache.commons.lang.StringEscapeUtils.escapeXml(renderContext.getRequest().getQueryString())
} else if (renderContext.getRequest().getAttribute("javax.servlet.forward.query_string") != null) {
    print "?" + org.apache.commons.lang.StringEscapeUtils.escapeXml(renderContext.getRequest().getAttribute("javax.servlet.forward.query_string"))
}