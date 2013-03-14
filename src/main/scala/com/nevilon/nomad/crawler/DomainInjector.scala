package com.nevilon.nomad.crawler


import com.nevilon.nomad.storage.graph.SynchronizedDBService


class DomainInjector(dbService: SynchronizedDBService) {

  def inject(url: String) {
    val normalizedUrl = URLUtils.normalize(url)
    val domainStr = URLUtils.getDomainName(normalizedUrl)
    dbService.createDomainIfNeeded(new Domain(domainStr, DomainStatus.NEW))
    dbService.getUrl(normalizedUrl) match {
      case None => {
        val url = dbService.saveOrUpdateUrl(new Url(normalizedUrl, UrlStatus.NEW))
        dbService.addUrlToDomain(url)
      }
      case Some(v) => //should exists
    }


  }

}
