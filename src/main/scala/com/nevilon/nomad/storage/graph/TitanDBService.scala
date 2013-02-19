package com.nevilon.nomad.storage.graph

import scala.Predef._
import com.nevilon.nomad._
import com.tinkerpop.blueprints.Vertex
import crawler.{Url, Relation}
import java.util.UUID
import scala.Some
import scala.collection.JavaConversions._
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion
import com.thinkaurelius.titan.core.{TitanTransaction, TitanGraph}

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/18/13
 * Time: 3:49 AM
 */


class TitanDBService(recreateDb: Boolean) {

  private val connector = new CassandraGraphStorageConnector
  private val graph = connector.getGraph

  def shutdown() {
    synchronized {
      connector.shutdown()
    }
  }


  private def getUrl(url: String, graph2:TitanTransaction): Option[Vertex] = {
   // synchronized {
      val vertices = graph2.getVertices("location", url)
      if (vertices.isEmpty) {
        None
      } else {
        if (vertices.size > 1) {
          throw new RuntimeException("There are more than one page with this url!")
        } else {
          Some(vertices.iterator.next())
        }
      }
  //  }
  }

  def linkUrls(relations: List[Relation]) {
    val tx = graph.startTransaction()

    def getOrCreate(url: Url): Vertex = {
      getUrl(url.location,tx) match {
        case Some(v) => v
        case None => saveOrUpdateOnGraph(url,tx)
      }
    }

    synchronized {
      relations.foreach(relation => {
        val parentPage = getOrCreate(relation.from)
        val childPage = getOrCreate(relation.to)
        tx.addEdge(UUID.randomUUID().toString, parentPage, childPage, "relation")
      })
    }
    tx.stopTransaction(Conclusion.SUCCESS)
  }


  private def saveOrUpdateOnGraph(url:Url, graph:TitanTransaction):Vertex = {
    val vertex = {
      getUrl(url.location,graph) match {
        case None => {

          graph.addVertex()
        }
        case Some(v) => {
          v
        }
      }
    }

    vertex.setProperty("status", url.status.toString)
    vertex.setProperty("location", url.location)
    vertex.setProperty("fileId", url.fileId)
    vertex
  }

  def saveOrUpdateUrl(url: Url): Vertex = {
    synchronized {
      val tx = graph.startTransaction()
      val vertex = {
        getUrl(url.location,tx) match {
          case None => {

            tx.addVertex()
          }
          case Some(v) => {
            v
          }
        }
      }

      vertex.setProperty("status", url.status.toString)
      vertex.setProperty("location", url.location)
      vertex.setProperty("fileId", url.fileId)
      tx.stopTransaction(Conclusion.SUCCESS)
      vertex

    }
  }


  def getBFSLinks(url: String, limit: Int): List[Url] = {
   // synchronized {
   val tx = graph.startTransaction()
      val rootVertex = getUrl(url,tx).get
      val traverser = new BFSTraverser(rootVertex, limit)
     val list =  traverser.traverse()
    tx.stopTransaction(Conclusion.SUCCESS)
    list
 //   }
  }


}