def filterUrl(url) {
   // if (url.contains(".pdf")) {
   //     return false
   // }
    if (url.contains("http://aclweb.org/adminwiki/") || url.contains("http://aclweb.org/aclwiki")) {
        return false
    }
    if (url.contains("http://nlp.stanford.edu/mediawiki/")){
        return false
    }
    if (url.contains("http://nlp.stanford.edu/nlp/javadoc")){
        return false
    }
    return true
}

def filterEntity(size, url, mimeType) {
    if (size > 1000000) {
        return false
    }
    return true

}