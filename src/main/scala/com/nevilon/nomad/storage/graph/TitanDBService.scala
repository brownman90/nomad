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
import logs.Logs
import scala.Predef.String

import collection.mutable
import scala.Some


class TitanDBService extends TransactionSupport with Logs {

  private val conf = GlobalConfig

  private val connector =
    conf.titanConfig.backend match {
      case "cassandra" => new CassandraGraphStorageConnector(conf.cassandraConfig)
      case "inmemory" => new InMemoryGraphStorageConnector(conf.inMemoryConfig)
      case "berkeley" => new BerkeleyGraphStorageConnector(conf.berkeleyConfig)
      case _ => {
        error("wrong backend configuration")
        throw new Error
      }
    }
  private implicit val graph = connector.getGraph

  val urlService = new UrlService
  val domainService = new DomainService
  domainService.createSuperDomainIfNeeded

  def shutdown() {
    connector.shutdown()
  }

  def linkUrls(relations: List[Relation]) {
    withTransaction {
      implicit tx => {
        val cache = new mutable.HashMap[String, Vertex]

        def getOrCreate(url: Url): Vertex = {
          cache.getOrElse(url.location, {
            urlService.getUrlInTx(url.location) match {
              case Some(v) => {
                cache.put(url.location, v)
                v
              }
              case None => {
                val v = urlService.saveOrUpdateUrlInTx(url)
                cache.put(url.location, v)
                v
              }
            }
          })
        }


        val isLinkedCache = new mutable.HashSet[String]

        var firstTime = 0l
        var secondTime = 0l

        implicit val superNode = domainService.getSuperDomainNodeInTx
        relations.foreach(relation => {
          var startTime = System.currentTimeMillis()
          val parentPage = getOrCreate(relation.from)
          val childPage = getOrCreate(relation.to)
          tx.addEdge("", parentPage, childPage, "relation")
          var endTime = System.currentTimeMillis()
          firstTime = firstTime + (endTime - startTime)

          val newChildUrl: Url = Transformers.vertex2Url(childPage)

          val domainName = URLUtils.getDomainName(URLUtils.normalize(URLUtils.getDomainName(newChildUrl.location)))
          val domain = new Domain(domainName, DomainStatus.NEW)
          startTime = System.currentTimeMillis()
          if (newChildUrl.status == UrlStatus.NEW && !isLinkedCache.contains(newChildUrl.location) &&
            !domainService.isUrlLinkedToDomainInTx(newChildUrl)) {
            domainService.addUrlToDomainInTx(domain, childPage)
            isLinkedCache.add(newChildUrl.location)
          }
          endTime = System.currentTimeMillis()
          secondTime = secondTime + (endTime - startTime)

        })
        println("firstTime " + firstTime)
        println("secondTime " + secondTime)
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