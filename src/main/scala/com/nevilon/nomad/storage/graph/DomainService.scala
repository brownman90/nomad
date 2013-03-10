package com.nevilon.nomad.storage.graph

import com.thinkaurelius.titan.core.{TitanTransaction, TitanGraph}
import com.nevilon.nomad.crawler._
import com.tinkerpop.blueprints.{Direction, Vertex, Element}
import com.tinkerpop.gremlin.java.GremlinPipeline
import collection.mutable.ListBuffer

import scala.collection.JavaConversions._
import scala.Some


//import Transformers._

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 3/5/13
 * Time: 2:43 PM
 */

class DomainService(implicit graph: TitanGraph) extends TransactionSupport {


  private val superDomainName = "supernode"


  def updateDomain(domain: Domain) {
    withTransaction {
      implicit tx => {
        getDomain(domain) match {
          case Some(v) => v.setProperty("status", domain.status.toString)
          case None =>
        }
      }
    }
  }

  def getDomainWithStatus(domainStatus: DomainStatus.Value): Iterator[Domain] = {
    withTransaction {
      implicit tx => {
        val superNodeVertex = getSuperDomainNode
        val it = superNodeVertex.getVertices(Direction.OUT).iterator()
        for (v <- it.seq if v.getProperty("status").toString == DomainStatus.NEW.toString) yield Transformers.vertex2Domain(v)
      }
    }
  }

  //URLUtils.getDomainName(URLUtils.normalize(url))
  def getLinksToCrawl(domain: Domain, limit: Int): List[Url] = {
    withTransaction[List[Url]] {
      implicit tx => {
        getUnprocessedLinks(domain, limit)
      }
    }
  }

  def createDomainIfNeeded(domain: Domain) {
    withTransaction {
      implicit tx => {
        getDomain(domain) match {
          case Some(v) => //exists
          case None => {
            //need to create
            val superNodeVertex = getSuperDomainNode
            val domainVertex = tx.addVertex()
            domainVertex.setProperty("domain", domain.name)
            domainVertex.setProperty("status", domain.status)
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


  //pass url, not domain!!!!
  def isUrlLinkedToDomain(url: String)(implicit tx: TitanTransaction, superNodeVertex: Vertex): Boolean = {
    val tmp = tx.getVertex("location", url).getEdges(Direction.IN, "link")
    tmp.iterator().foreach(e => {
      println(e.getLabel)
    })
    val linkedCount = tmp.iterator().size
    require(linkedCount == 0 || linkedCount == 1)
    linkedCount == 1
  }

  def _getUrlFromDomainPipe(location: String, domain: Domain)(implicit tx: TitanTransaction, superNodeVertex: Vertex): Option[Element] = {
    val superNodeVertex = getSuperDomainNode
    val pipe = new GremlinPipeline(superNodeVertex).
      out("link").
      has("domain", domain.name).
      out("link").
      has("location", location)
    if (pipe.iterator().nonEmpty) {
      Some(pipe.iterator().next())
    } else None
  }

  def getSuperDomainNode(implicit tx: TitanTransaction): Vertex = {
    tx.getVertex("domain", superDomainName)
  }

  private def getUnprocessedLinks(domain: Domain, limit: Int)(implicit tx: TitanTransaction): List[Url] = {
    val superNodeVertex = getDomain(domain).get
    val pipe = new GremlinPipeline(superNodeVertex).
      out("link").
      has("status", UrlStatus.NEW.toString).
      range(0, limit)


    val urls = new ListBuffer[Url]
    pipe.iterator().foreach(v => {
      urls += Transformers.vertex2Url(v)
    })
    urls.toList
  }


  def getDomain(domain: Domain)(implicit tx: TitanTransaction): Option[Element] = {
    val superNodeVertex = tx.getVertex("domain", superDomainName)
    val pipe = new GremlinPipeline(superNodeVertex).
      out("link").
      has("domain", domain.name)

    if (pipe.iterator().hasNext) Some(pipe.iterator().next()) else None
  }


  /*

    val it = tx.getVertex("location", "http://lenta.ru").getEdges(Direction.IN).iterator()
    //  it.size
    while (it.hasNext) {
      val e = it.next()
      println(e.getLabel)
    }

    DO NOT USE foreach with  Blueprints!!!!

   */

  def addUrlToDomain(domain: Domain, urlVertex: Vertex)(implicit tx: TitanTransaction) {
    val domainVertex = tx.getVertex("domain", domain.name)
    tx.addEdge("", domainVertex, urlVertex, "link")

  }

  def removeUrlFromDomainInTx(location: String, domain: Domain)(implicit tx: TitanTransaction) {
    val it = tx.getVertex("location", location).getEdges(Direction.IN, "link").iterator()
    if (it.hasNext) {
      tx.removeEdge(it.next())
    }


    /*
    implicit val superNode = getSuperDomainNode
    getUrlFromDomainPipe(location, domain) match {
      case None => //nothing to do, skip
      case Some(element) => {
        println(element.getId)
        println(tx.getEdge(element.getId))
        println(tx.getVertex(element.getId).getEdges(Direction.OUT,"link").iterator().size)
        tx.removeEdge(tx.getEdge(element.getId))
      }
    }
    */
  }


}
