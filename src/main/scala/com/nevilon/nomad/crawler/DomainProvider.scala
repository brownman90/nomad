package com.nevilon.nomad.crawler

import com.nevilon.nomad.storage.graph.{SynchronizedDBService, DomainService}
import org.apache.commons.lang.builder.{HashCodeBuilder, EqualsBuilder}
import com.tinkerpop.blueprints.Element

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 3/8/13
 * Time: 12:04 PM
 */

class DomainInjector(dbService: SynchronizedDBService) {

  def inject(url: String) {
    val normalizedUrl = URLUtils.normalize(url)
    val domainStr = URLUtils.getDomainName(normalizedUrl)
    dbService.createDomainIfNeeded(new Domain(domainStr, DomainStatus.NEW))
    dbService.getUrl(normalizedUrl) match {
      case None => {
        dbService.saveOrUpdateUrl(new Url(normalizedUrl, UrlStatus.NEW))
        dbService.addUrlToDomain(normalizedUrl)
      }
      case Some(v) => //should exists
    }


  }

}




