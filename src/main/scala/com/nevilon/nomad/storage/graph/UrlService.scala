package com.nevilon.nomad.storage.graph

import com.thinkaurelius.titan.core.{TitanTransaction, TitanGraph}
import com.nevilon.nomad.crawler.Url
import com.tinkerpop.blueprints.Vertex

import scala.collection.JavaConversions._

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 3/5/13
 * Time: 2:44 PM
 */
class UrlService(implicit graph: TitanGraph) extends TransactionSupport {


  def saveOrUpdateUrl(url: Url) = {
    withTransaction {
      implicit tx => {
        saveOrUpdateUrlInTx(url)
      }
    }
  }


  //TODO return Url, not raw vertex
  def getUrl(url: String): Option[Vertex] =  {
    withTransaction[Option[Vertex]] {
      implicit tx =>
        getUrlInTx(url)
    }
  }

  def saveOrUpdateUrlInTx(url: Url)(implicit tx: TitanTransaction): Vertex = {
    val vertex = {
      getUrlInTx(url.location) match {
        case None => {
          val newV = tx.addVertex()
          newV.setProperty("location", url.location)
          newV
        }
        case Some(v) => v
      }
    }

    vertex.setProperty("status", url.status.toString)

    vertex.setProperty("fileId", url.fileId)

    vertex
  }

  def getUrlInTx(url: String)(implicit tx: TitanTransaction): Option[Vertex] = {
    val vertices = tx.getVertices("location", url)
    if (vertices.isEmpty) None
    else if (vertices.size > 1)
      throw new RuntimeException("There are more than one page with this url!")
    else Some(vertices.iterator.next())
  }


}

