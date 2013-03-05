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
import com.tinkerpop.blueprints.{Direction, Element, Vertex}
import crawler._
import java.util.UUID
import logs.Logs
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion
import com.thinkaurelius.titan.core.{TitanGraph, TitanTransaction}
import scala.Predef.String
import scala.collection.JavaConversions._
import com.tinkerpop.gremlin.java.GremlinPipeline
import scala.Some
import collection.mutable.ListBuffer


trait TitanDBService extends Logs with TransactionSupport {

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
  val domainService = new DomainService


  domainService.createSuperDomainIfNeeded

  def shutdown() {
    synchronized {
      connector.shutdown()
    }
  }


  def linkUrls(relations: List[Relation]) {
    synchronized {
      withTransaction {
        implicit tx => {

          def getOrCreate(url: Url): Vertex = {
            getUrlInTx(url.location) match {
              case Some(v) => v
              case None => saveOrUpdateUrlInTx(url)
            }
          }

          relations.foreach(relation => {
            val parentPage = getOrCreate(relation.from)
            val childPage = getOrCreate(relation.to)
            tx.addEdge(UUID.randomUUID().toString, parentPage, childPage, "relation")

            import Transformers.vertex2Url
            val newChildUrl: Url = childPage


            val domain = URLUtils.getDomainName(URLUtils.normalize(URLUtils.getDomainName(newChildUrl.location)))
            if (newChildUrl.status == UrlStatus.NEW && !domainService.isUrlLinkedToDomain(newChildUrl.location, domain)) {
              //child page = NEW and not linked with domain
              domainService.addUrlToDomain(domainService.getDomain(domain).get.getId.asInstanceOf[Long], childPage)
              println("linked")
            } else {
              println("not linked!")
            }
          })

        }
      }
    }
  }


  def saveOrUpdateUrl(url: Url) = {
    synchronized {
      withTransaction {
        implicit tx => {
          saveOrUpdateUrlInTx(url)
        }
      }
    }
  }


  def getBFSLinks(url: String, limit: Int): List[Url] = {
    synchronized {
      withTransaction[List[Url]] {
        implicit tx => {
          domainService.getLinksFFF(URLUtils.getDomainName(URLUtils.normalize(url)), 500)
          // val rootVertex = getUrlInTx(url).get
          // new BFSTraverser(rootVertex, limit).traverse()
        }
      }
    }
  }

  def getUrl(url: String): Option[Vertex] = {
    synchronized {
      withTransaction[Option[Vertex]] {
        implicit tx =>
          getUrlInTx(url)
      }
    }
  }

  private def saveOrUpdateUrlInTx(url: Url)(implicit tx: TitanTransaction): Vertex = {
    synchronized {
      val vertex = {
        getUrlInTx(url.location) match {
          case None => tx.addVertex()
          case Some(v) => v
        }
      }

      vertex.setProperty("status", url.status.toString)
      vertex.setProperty("location", url.location)
      vertex.setProperty("fileId", url.fileId)
      // element.setProperty("domain", url.getDomain)

      vertex
    }
  }

  private def getUrlInTx(url: String)(implicit tx: TitanTransaction): Option[Vertex] = {
    synchronized {
      val vertices = tx.getVertices("location", url)
      if (vertices.isEmpty) None
      else if (vertices.size > 1)
        throw new RuntimeException("There are more than one page with this url!")
      else Some(vertices.iterator.next())
    }
  }

  def removeUrlFromDomain(location: String, domain: String) {
    synchronized {
      withTransaction {
        implicit tx => {
          domainService.removeUrlFromDomain(location, domain)
        }
      }
    }
  }


  def addUrlToDomain(location: String) {
    synchronized {
      withTransaction {
        implicit tx => {
          val domain = URLUtils.getDomainName(URLUtils.normalize(location))
          val domainId = domainService.getDomain(domain).get.getId

          domainService.addUrlToDomain(domainId.asInstanceOf[Long], getUrlInTx(location).get)
        }
      }
    }
  }


}

class DomainService(implicit graph: TitanGraph) extends TransactionSupport {


  private val superDomainName = "supernode"


  def createDomainIfNeeded(domain: String) {
    synchronized {
      withTransaction {
        implicit tx => {
          getDomain(domain) match {
            case Some(v) => //exists
            case None => {
              //need to create
              val superNodeVertex = getSuperDomainNode
              val domainVertex = tx.addVertex()
              domainVertex.setProperty("domain", domain)
              tx.addEdge("", superNodeVertex, domainVertex, "link")
            }
          }
        }
      }
    }
  }

  def createSuperDomainIfNeeded {
    synchronized {
      withTransaction {
        implicit tx => {
          if (tx.getVertex("domain", superDomainName) == null) {
            tx.addVertex().setProperty("domain", superDomainName)
          }
        }
      }
    }
  }

  //
  //  def _test() {
  //    withTransaction {
  //      implicit tx => {
  //        val superDomainNode = tx.getVertex("domain", superDomainName)
  //
  //        var domainVertex = tx.addVertex()
  //        domainVertex.setProperty("domain", "google.com")
  //        tx.addEdge("", superDomainNode, domainVertex, "link")
  //
  //        domainVertex = tx.addVertex()
  //        domainVertex.setProperty("domain", "lenta.ru")
  //        tx.addEdge("", superDomainNode, domainVertex, "link")
  //
  //        for (a <- 1 to 500) {
  //          val url = new Url("somefile.html " + a, UrlStatus.COMPLETE, "", "")
  //          val urlV = saveOrUpdateUrl(url)
  //          tx.addEdge("", domainVertex, tx.getVertex(urlV.getId), "link")
  //        }
  //
  //
  //        getLinksFFF("lenta.ru", 500)
  //      }
  //    }
  //  }

  def isUrlLinkedToDomain(location: String, domain: String)(implicit tx: TitanTransaction): Boolean = {
    getUrlFromDomainPipe(location, domain) match {
      case None => false
      case Some(url) => true
    }
  }

  def getUrlFromDomainPipe(location: String, domain: String)(implicit tx: TitanTransaction): Option[Element] = {
    val superNodeVertex = getSuperDomainNode
    val pipe = new GremlinPipeline(superNodeVertex).
      out("link").
      has("domain", domain).
      out("link").
      has("location", location)
    if (pipe.iterator().nonEmpty) {
      Some(pipe.iterator().next())
    } else None
  }

  def getSuperDomainNode(implicit tx: TitanTransaction): Vertex = {
    tx.getVertex("domain", superDomainName)
  }

  def getLinksFFF(domain: String, limit: Int)(implicit tx: TitanTransaction): List[Url] = {
    val superNodeVertex = getDomain(domain).get
 //   println(tx.getVertex(superNodeVertex.getId).getEdges(Direction.OUT).iterator().next().getLabel)
    val pipe = new GremlinPipeline(superNodeVertex).
      out("link").
     // has("domain", domain)//.
    //  out("link").
      has("status", UrlStatus.NEW.toString).
      range(0, limit)

    import Transformers.vertex2Url
    val urls = new ListBuffer[Url]
    pipe.iterator().foreach(v => urls += v)
    urls.toList
  }


  def getDomain(domain: String)(implicit tx: TitanTransaction): Option[Element] = {
    val superNodeVertex = tx.getVertex("domain", superDomainName)
    val pipe = new GremlinPipeline(superNodeVertex).
      out("link").
      has("domain", domain)

    if (pipe.iterator().hasNext) Some(pipe.iterator().next()) else None
  }


  def addUrlToDomain(domainVertexId: Long, urlVertex: Vertex)(implicit tx: TitanTransaction) {
    val domainVertex = tx.getVertex(domainVertexId)
    tx.addEdge("", domainVertex, urlVertex, "link")
  }

  def removeUrlFromDomain(location: String, domain: String)(implicit tx: TitanTransaction) {
    getUrlFromDomainPipe(location, domain) match {
      case None => //nothing to do, skip
      case Some(element) => {
        tx.removeEdge(tx.getEdge(element.getId))
      }
    }
  }


}

trait TransactionSupport {


  def withTransaction[T](f: TitanTransaction => T)(implicit implGraph: TitanGraph): T = {
    val tx = implGraph.startTransaction()
    try {
      val result = f(tx)
      tx.stopTransaction(Conclusion.SUCCESS)
      result
    }
    catch {
      case ex: Throwable => {
        ex.printStackTrace()
        tx.abort()
        throw ex
      }
    }
  }

}
