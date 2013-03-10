/**
 * Copyright (C) 2012-2013 Vadim Bartko (vadim.bartko@nevilon.com).
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * See file LICENSE.txt for License information.
 */
package com.nevilon.nomad.storage.graph

import com.nevilon.nomad.crawler.{Domain, DomainStatus, Relation, Url}
import com.nevilon.nomad.devtools.Prototypes

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

  def getUrl(url: String): Option[Url] = synchronized {
    Prototypes.timed({
      titanDBService.urlService.getUrl(url)
    }, "getUrl")

  }

  def saveOrUpdateUrl(url: Url):Url = synchronized {
    //    Prototypes.timed({
    titanDBService.urlService.saveOrUpdateUrl(url)
    //  }, "saveOrUpdateUrl")

  }

  def addUrlToDomain(url: Url):Unit = synchronized {
    Prototypes.timed({
      titanDBService.addUrlToDomain(url)
    }, "addUrlToDomain")
  }

  def createDomainIfNeeded(domain: Domain):Unit = synchronized {
    Prototypes.timed({
      titanDBService.domainService.createDomainIfNeeded(domain)
    }, "createDomainIfNeeded")


  }

  def removeUrlFromDomain(url: Url):Unit = synchronized {
    Prototypes.timed({
      titanDBService.removeUrlFromDomain(url)
    }, "removeUrlFromDomain")
  }


  def getDomainWithStatus(domainStatus: DomainStatus.Value): Option[Domain] = synchronized {
    titanDBService.domainService.getDomainWithStatus(domainStatus)
  }

  def updateDomain(domain: Domain):Unit = synchronized {
    titanDBService.domainService.updateDomain(domain)
  }


}
