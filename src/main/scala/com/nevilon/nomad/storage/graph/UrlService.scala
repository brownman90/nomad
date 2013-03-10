package com.nevilon.nomad.storage.graph

import com.thinkaurelius.titan.core.{TitanTransaction, TitanGraph}
import com.nevilon.nomad.crawler.{Transformers, Url}
import com.tinkerpop.blueprints.Vertex

import scala.collection.JavaConversions._

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 3/5/13
 * Time: 2:44 PM
 */
class UrlService(implicit graph: TitanGraph) extends TransactionSupport {


  def saveOrUpdateUrl(url: Url): Url = {
    withTransaction {
      implicit tx => {
        Transformers.vertex2Url(saveOrUpdateUrlInTx(url))
      }
    }
  }


  def getUrl(location: String): Option[Url] = {
    withTransaction[Option[Url]] {
      implicit tx =>
        getUrlInTx(location) match {
          case None => None
          case Some(v) => Some(Transformers.vertex2Url(v))
        }
    }
  }

  def saveOrUpdateUrlInTx(url: Url)(implicit tx: TitanTransaction): Vertex = {
    val vertex = {
      getUrlInTx(url.location) match {
        case None => {
          val newV = tx.addVertex()
          newV.setProperty(GraphProperties.Url.locationProperty, url.location)
          newV
        }
        case Some(v) => v
      }
    }
    vertex.setProperty(GraphProperties.Url.statusProperty, url.status.toString)
    vertex.setProperty(GraphProperties.Url.fileIdProperty, url.fileId)
    vertex
  }

  def getUrlInTx(location: String)(implicit tx: TitanTransaction): Option[Vertex] = {
    val vertex = tx.getVertex(GraphProperties.Url.locationProperty, location)
    if (vertex == null) None else Some(vertex)
  }


}