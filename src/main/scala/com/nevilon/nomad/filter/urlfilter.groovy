def filterUrl(url){
    if(url.contains(".pdf")){
        return false
    }
    if(url.contains("http://aclweb.org/adminwiki/") || url.contains("http://aclweb.org/aclwiki")){
        return false
    }
    return true
}