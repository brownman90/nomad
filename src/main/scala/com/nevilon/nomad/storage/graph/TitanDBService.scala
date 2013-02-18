package com.nevilon.nomad.storage.graph

import scala.Predef._
import com.nevilon.nomad._
import com.tinkerpop.blueprints.Vertex
import crawler.{Url, Relation}
import java.util.UUID
import scala.Some
import scala.collection.JavaConversions._
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion

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


  private def getUrl(url: String): Option[Vertex] = {
   // synchronized {
      val vertices = graph.getVertices("location", url)
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
    def getOrCreate(url: Url): Vertex = {
      getUrl(url.location) match {
        case Some(v) => v
        case None => saveOrUpdateUrl(url)
      }
    }
    synchronized {
      relations.foreach(relation => {
        val parentPage = getOrCreate(relation.from)
        val childPage = getOrCreate(relation.to)
        graph.addEdge(UUID.randomUUID().toString, parentPage, childPage, "relation")
      })
    }
    graph.stopTransaction(Conclusion.SUCCESS)
  }


  def saveOrUpdateUrl(url: Url): Vertex = {
    synchronized {

      val vertex = {
        getUrl(url.location) match {
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
  }


  def getBFSLinks(url: String, limit: Int): List[Url] = {
   // synchronized {
      val rootVertex = getUrl(url).get
      val traverser = new BFSTraverser(rootVertex, limit)
      traverser.traverse()
 //   }
  }


}