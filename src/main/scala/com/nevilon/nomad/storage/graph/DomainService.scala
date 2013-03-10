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

import com.thinkaurelius.titan.core.{TitanTransaction, TitanGraph}
import com.nevilon.nomad.crawler._
import com.tinkerpop.blueprints.{Direction, Vertex}
import com.tinkerpop.gremlin.java.GremlinPipeline
import collection.mutable.ListBuffer

import scala.collection.JavaConversions._
import scala.Some


class DomainService(implicit graph: TitanGraph) extends TransactionSupport {


  private val superDomainName = "supernode"

  def updateDomain(domain: Domain) {
    withTransaction {
      implicit tx => {
        getDomainInTx(domain) match {
          case Some(v) => v.setProperty(GraphProperties.Domain.statusProperty, domain.status.toString)
          case None =>
        }
      }
    }
  }

  def getDomainWithStatus(domainStatus: DomainStatus.Value): Iterator[Domain] = {
    withTransaction {
      implicit tx => {
        val superNodeVertex = getSuperDomainNodeInTx
        val it = superNodeVertex.getVertices(Direction.OUT).iterator()
        for (v <- it.seq if v.getProperty(GraphProperties.Domain.statusProperty).toString == DomainStatus.NEW.toString)
        yield Transformers.vertex2Domain(v)
      }
    }
  }

  def getLinksToCrawl(domain: Domain, limit: Int): List[Url] = {
    withTransaction[List[Url]] {
      implicit tx => {
        getUnprocessedLinksInTx(domain, limit)
      }
    }
  }

  def createDomainIfNeeded(domain: Domain) {
    withTransaction {
      implicit tx => {
        getDomainInTx(domain) match {
          case Some(v) => //exists
          case None => {
            //need to create
            val superNodeVertex = getSuperDomainNodeInTx
            val domainVertex = tx.addVertex()
            domainVertex.setProperty(GraphProperties.Domain.nameProperty, domain.name)
            domainVertex.setProperty(GraphProperties.Domain.statusProperty, domain.status.toString)
            tx.addEdge("", superNodeVertex, domainVertex, GraphProperties.Domain.urlEdgeLabel)
          }
        }
      }
    }
  }

  def createSuperDomainIfNeeded {
    withTransaction {
      implicit tx => {
        if (tx.getVertex(GraphProperties.Domain.nameProperty, superDomainName) == null) {
          tx.addVertex().setProperty(GraphProperties.Domain.nameProperty, superDomainName)
        }
      }
    }
  }

  def isUrlLinkedToDomainInTx(url: Url)(implicit tx: TitanTransaction): Boolean = {
    val linksIt = tx.getVertex(GraphProperties.Url.locationProperty, url.location).
      getEdges(Direction.IN, GraphProperties.Domain.urlEdgeLabel).iterator()
    val linkedCount = linksIt.size
    require(linkedCount == 0 || linkedCount == 1)
    linkedCount == 1
  }

  def getSuperDomainNodeInTx(implicit tx: TitanTransaction): Vertex = {
    tx.getVertex(GraphProperties.Domain.nameProperty, superDomainName)
  }

  private def getUnprocessedLinksInTx(domain: Domain, limit: Int)(implicit tx: TitanTransaction): List[Url] = {
    val superNodeVertex = getDomainInTx(domain).get
    val pipe = new GremlinPipeline(superNodeVertex).
      out(GraphProperties.Domain.urlEdgeLabel).
      has(GraphProperties.Url.statusProperty, UrlStatus.NEW.toString).
      range(0, limit)
    val urls = new ListBuffer[Url]
    pipe.iterator().foreach(v => urls += Transformers.vertex2Url(v))
    urls.toList
  }


  def getDomainInTx(domain: Domain)(implicit tx: TitanTransaction): Option[Vertex] = {
    val v = tx.getVertex(GraphProperties.Domain.nameProperty, domain.name)
    if (v == null) None else Some(v)
  }


  def addUrlToDomainInTx(domain: Domain, urlVertex: Vertex)(implicit tx: TitanTransaction) {
    val domainVertex = tx.getVertex(GraphProperties.Domain.nameProperty, domain.name)
    tx.addEdge("", domainVertex, urlVertex, GraphProperties.Domain.urlEdgeLabel)
  }

  def removeUrlFromDomainInTx(url: Url)(implicit tx: TitanTransaction) {
    val it = tx.getVertex(GraphProperties.Url.locationProperty, url.location).
      getEdges(Direction.IN, GraphProperties.Domain.urlEdgeLabel).
      iterator()
    if (it.hasNext) tx.removeEdge(it.next())
  }


}

//DO NOT USE foreach with  Blueprints!!!!
//iterator.size moves pointer to end
