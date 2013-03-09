package com.nevilon.nomad.storage.graph

import com.nevilon.nomad.crawler.{Domain, DomainStatus, Relation, Url}
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

  def getLinksToCrawl(domain: Domain, limit: Int): List[Url] = synchronized {
    Prototypes.timed({
      titanDBService.domainService.getLinksToCrawl(domain, limit)
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

  def createDomainIfNeeded(domain: Domain) = synchronized {
    Prototypes.timed({
      titanDBService.domainService.createDomainIfNeeded(domain)
    }, "createDomainIfNeeded")


  }

  def removeUrlFromDomain(location: String, domain: Domain) = synchronized {
    Prototypes.timed({
      titanDBService.removeUrlFromDomain(location, domain)
    }, "removeUrlFromDomain")
  }


  def getDomainWithStatus(domainStatus: DomainStatus.Value): Option[Domain] = synchronized {
    titanDBService.domainService.getDomainWithStatus(domainStatus)
  }

  def updateDomain(domain: Domain) = synchronized {
    titanDBService.domainService.updateDomain(domain)
  }


}
