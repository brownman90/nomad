package com.nevilon.nomad.storage.graph

import com.nevilon.nomad.crawler.{Relation, Url}
import com.nevilon.nomad.devtools.Prototypes

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 3/5/13
 * Time: 2:45 PM
 */
class SynchronizedDBService {

  private val titanDBService = new TitanDBService

  def shutdown = synchronized {
    titanDBService.shutdown()
  }

  def getLinksToCrawl(url: String, limit: Int): List[Url] = synchronized {
    Prototypes.timed({
      titanDBService.domainService.getLinksToCrawl(url, limit)
    }, "getLinksToCrawl")

  }

  def linkUrls(relations: List[Relation]) = synchronized {
    Prototypes.timed({
      titanDBService.linkUrls(relations)
    }, "linkUrls")
  }

  def getUrl(url: String) = synchronized {
    Prototypes.timed({
      titanDBService.urlService.getUrl(url)
    }, "getUrl")

  }

  def saveOrUpdateUrl(url: Url) = synchronized {
//    Prototypes.timed({
      titanDBService.urlService.saveOrUpdateUrl(url)
  //  }, "saveOrUpdateUrl")

  }

  def addUrlToDomain(location: String) = synchronized {
    Prototypes.timed({
      titanDBService.addUrlToDomain(location)
    }, "addUrlToDomain")
  }

  def createDomainIfNeeded(domain: String) = synchronized {
    Prototypes.timed({
      titanDBService.domainService.createDomainIfNeeded(domain)
    }, "createDomainIfNeeded")


  }

  def removeUrlFromDomain(location: String, domain: String) = synchronized {
    Prototypes.timed({
      titanDBService.removeUrlFromDomain(location, domain)
    }, "removeUrlFromDomain")


  }


}
