import org.apache.commons.lang.StringEscapeUtils

if(currentNode.hasProperty("j:keywords")){
    keywords = currentNode.getProperty("j:keywords").getValues();
    keywordsSize =  keywords.size();
    for(int i = 0 ; i < keywordsSize ; i++){
        print StringEscapeUtils.escapeHtml(keywords.value[i].getString());
        if(keywordsSize > 0 && i < keywordsSize-1){
            print ", ";
        }
    }
} else {
    print "Defined Keyword(s) before use !";
}
