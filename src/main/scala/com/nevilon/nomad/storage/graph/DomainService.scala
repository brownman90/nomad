package com.nevilon.nomad.storage.graph

import com.thinkaurelius.titan.core.{TitanTransaction, TitanGraph}
import com.nevilon.nomad.crawler.{Transformers, UrlStatus, URLUtils, Url}
import com.tinkerpop.blueprints.{Vertex, Element}
import com.tinkerpop.gremlin.java.GremlinPipeline
import collection.mutable.ListBuffer

import scala.collection.JavaConversions._

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 3/5/13
 * Time: 2:43 PM
 */

class DomainService(implicit graph: TitanGraph) extends TransactionSupport {


  private val superDomainName = "supernode"


  def getLinksToCrawl(url: String, limit: Int): List[Url] = {
    withTransaction[List[Url]] {
      implicit tx => {
        getUnprocessedLinks(URLUtils.getDomainName(URLUtils.normalize(url)), limit)
      }
    }
  }

  def createDomainIfNeeded(domain: String) {
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

  def createSuperDomainIfNeeded {
    withTransaction {
      implicit tx => {
        if (tx.getVertex("domain", superDomainName) == null) {
          tx.addVertex().setProperty("domain", superDomainName)
        }
      }
    }
  }


  def isUrlLinkedToDomain(location: String, domain: String)(implicit tx: TitanTransaction, superNodeVertex: Vertex): Boolean = {
    getUrlFromDomainPipe(location, domain) match {
      case None => false
      case Some(url) => true
    }
  }

  def getUrlFromDomainPipe(location: String, domain: String)(implicit tx: TitanTransaction, superNodeVertex: Vertex): Option[Element] = {
    // val superNodeVertex = getSuperDomainNode
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

  private def getUnprocessedLinks(domain: String, limit: Int)(implicit tx: TitanTransaction): List[Url] = {
    val superNodeVertex = getDomain(domain).get
    val pipe = new GremlinPipeline(superNodeVertex).
      out("link").
      has("status", UrlStatus.NEW.toString).
      range(0, limit)

    import Transformers.vertex2Url
    val urls = new ListBuffer[Url]
    pipe.iterator().foreach(v => {
      urls += v
    })
    urls.toList
  }


  def getDomain(domain: String)(implicit tx: TitanTransaction): Option[Element] = {
    val superNodeVertex = tx.getVertex("domain", superDomainName)
    val pipe = new GremlinPipeline(superNodeVertex).
      out("link").
      has("domain", domain)

    if (pipe.iterator().hasNext) Some(pipe.iterator().next()) else None
  }


  def addUrlToDomain(domain: String, urlVertex: Vertex)(implicit tx: TitanTransaction) {
    val domainVertex = tx.getVertex("domain", domain)
    tx.addEdge("", domainVertex, urlVertex, "link")
  }

  def removeUrlFromDomainInTx(location: String, domain: String)(implicit tx: TitanTransaction) {
    implicit val superNode = getSuperDomainNode
    getUrlFromDomainPipe(location, domain) match {
      case None => //nothing to do, skip
      case Some(element) => {
        tx.removeEdge(tx.getEdge(element.getId))
      }
    }
  }


}
