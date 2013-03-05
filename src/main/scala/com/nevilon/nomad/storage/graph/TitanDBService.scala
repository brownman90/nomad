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
import java.util.UUID
import logs.Logs
import scala.Predef.String

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

        def getOrCreate(url: Url): Vertex = {
          urlService.getUrlInTx(url.location) match {
            case Some(v) => v
            case None => urlService.saveOrUpdateUrlInTx(url)
          }
        }

        implicit val superNode = domainService.getSuperDomainNode
        relations.foreach(relation => {
          val parentPage = getOrCreate(relation.from)
          val childPage = getOrCreate(relation.to)
          tx.addEdge(UUID.randomUUID().toString, parentPage, childPage, "relation")

          import Transformers.vertex2Url
          val newChildUrl: Url = childPage

          val domain = URLUtils.getDomainName(URLUtils.normalize(URLUtils.getDomainName(newChildUrl.location)))
          if (newChildUrl.status == UrlStatus.NEW && !domainService.isUrlLinkedToDomain(newChildUrl.location, domain)) {
            domainService.addUrlToDomain(domain, childPage)
          }
        })

      }
    }
  }

  def removeUrlFromDomain(location: String, domain: String) {
    withTransaction {
      implicit tx => {
        domainService.removeUrlFromDomainInTx(location, domain)
      }
    }
  }

  def addUrlToDomain(location: String) {
    withTransaction {
      implicit tx => {
        val domain = URLUtils.getDomainName(URLUtils.normalize(location))
        domainService.addUrlToDomain(domain, urlService.getUrlInTx(location).get)
      }
    }
  }


}