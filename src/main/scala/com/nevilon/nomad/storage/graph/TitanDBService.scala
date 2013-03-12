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

import scala.Predef._
import com.nevilon.nomad._
import boot.GlobalConfig
import com.tinkerpop.blueprints.Vertex
import crawler._
import devtools.Prototypes
import logs.Logs
import scala.Predef.String

import collection.mutable
import scala.Some


class TitanDBService extends TransactionSupport with Logs {

  private val connectorSelector = new ConnectorSelector
  private implicit val graph = connectorSelector.graph

  val urlService = new UrlService
  val domainService = new DomainService
  domainService.createSuperDomainIfNeeded


  def shutdown() =connectorSelector.shutdown()


  def linkUrls(relations: List[Relation]) {
    withTransaction {
      implicit tx => {
        val urlCache = new Cache[Url]((url) => {
          urlService.getUrlInTx(url.location)
        }, (url) => {
          Some(urlService.saveOrUpdateUrlInTx(url))
        })
        val domainCache = new Cache[Domain]((domain) => {
          domainService.getDomainInTx(domain)
        }, (domain) => Some(domainService.createDomainIfNeededInTx(domain)))


        val isLinkedCache = new mutable.HashSet[String]

        //var firstTime = 0l
        //var secondTime = 0l

        implicit val superNode = domainService.getSuperDomainNodeInTx
        relations.foreach(relation => {
          //   var startTime = System.currentTimeMillis()
          val parentPage = urlCache.getOrElse(relation.from).get //getOrCreate(relation.from)
          val childPage = urlCache.getOrElse(relation.to).get //getOrCreate(relation.to)
          tx.addEdge("", parentPage, childPage, "relation")
          //var endTime = System.currentTimeMillis()
          // firstTime = firstTime + (endTime - startTime)

          val newChildUrl: Url = Transformers.vertex2Url(childPage)

          val domainName = URLUtils.getDomainName(URLUtils.normalize(URLUtils.getDomainName(newChildUrl.location)))
          val domain = new Domain(domainName, DomainStatus.NEW)
          domainCache.getOrElse(domain).get //getOrCreateDomain(domain)
          // startTime = System.currentTimeMillis()
          if (newChildUrl.status == UrlStatus.NEW && !isLinkedCache.contains(newChildUrl.location) &&
            !domainService.isUrlLinkedToDomainInTx(newChildUrl)) {
            domainService.addUrlToDomainInTx(domain, childPage)
            isLinkedCache.add(newChildUrl.location)
          }
          // endTime = System.currentTimeMillis()
          // secondTime = secondTime + (endTime - startTime)

        })
        //    println("firstTime " + firstTime)
        //   println("secondTime " + secondTime)
      }
    }
  }

  def removeUrlFromDomain(url: Url) {
    withTransaction {
      implicit tx => {
        domainService.removeUrlFromDomainInTx(url)
      }
    }
  }

  def addUrlToDomain(url: Url) {
    withTransaction {
      implicit tx => {
        val domainName = URLUtils.getDomainName(URLUtils.normalize(url.location))
        val domain = new Domain(domainName, DomainStatus.NEW)
        domainService.addUrlToDomainInTx(domain, urlService.getUrlInTx(url.location).get)
      }
    }
  }


}