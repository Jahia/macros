if (renderContext.getRequest().getQueryString() != null) {
print "?" + URLEncoder.encode(URLDecoder.decode(renderContext.getRequest().getQueryString(), "UTF-8"), "UTF-8")
}