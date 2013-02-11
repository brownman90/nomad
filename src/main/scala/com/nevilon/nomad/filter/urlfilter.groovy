def filterUrl(url) {
    // if (url.contains(".pdf")) {
    //     return false
    // }
    if(url.contains("http://nlp.stanford.edu")){
        if (url.contains(".pdf") || url.contains(".tgz") || url.contains(".ppt") ){return false}

    }
    if (url.contains("http://consc.net/online/")) {
        return false
    }

    if (url.contains("http://aclweb.org/adminwiki/") || url.contains("http://aclweb.org/aclwiki")) {
        return false
    }
    if (url.contains("http://nlp.stanford.edu/mediawiki/")) {
        return false
    }
    if (url.contains("http://nlp.stanford.edu/nlp/javadoc") || url.contains("http://nlp.stanford.edu/nlpwiki/")) {
        return false
    }
    return true
}

def filterEntity(size, url, mimeType) {
    if (size > 50000) {
        return false
    }
    return true

}