def filterUrl(url) {
    // if (url.contains(".pdf")) {
    //     return false
    // }
    if (url.contains("http://aaai.org/ojs/index.php/aimagazine/search")


    ) {
        return false
    }
    if (url.contains("http://nlp.stanford.edu")) {
        if (  url.contains(".tgz")) {
            return false
        }

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
    if (size > 10000000) {
        return false
    }
    return true
}

def filterDomain(domain){
    if (domain.contains("arxiv.org")){
        return false
    }
    if (domain.contains(".edu")){
        return true
    }
    return false


}
