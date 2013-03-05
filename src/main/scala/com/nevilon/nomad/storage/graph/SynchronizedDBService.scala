package com.nevilon.nomad.storage.graph

import com.nevilon.nomad.crawler.{Relation, Url}
import com.tinkerpop.blueprints.Vertex

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 3/5/13
 * Time: 2:45 PM
 */
class SynchronizedDBService {

  private val titanDBService = new TitanDBService {}

  def shutdown = synchronized {
    titanDBService.shutdown()
  }

  def getLinksToCrawl(url: String, limit: Int): List[Url] = synchronized {
    titanDBService.domainService.getLinksToCrawl(url, limit)
  }

  def linkUrls(relations: List[Relation]) = synchronized {
    titanDBService.linkUrls(relations)
  }

  def getUrl(url: String): Option[Vertex] = synchronized {
    titanDBService.urlService.getUrl(url)
  }

  def saveOrUpdateUrl(url: Url) = synchronized {
    titanDBService.urlService.saveOrUpdateUrl(url)
  }

  def addUrlToDomain(location: String) = synchronized {
    titanDBService.addUrlToDomain(location)
  }

  def createDomainIfNeeded(domain: String) = synchronized {
    titanDBService.domainService.createDomainIfNeeded(domain)
  }

  def removeUrlFromDomain(location: String, domain: String) = synchronized {
    titanDBService.removeUrlFromDomain(location, domain)
  }


}
