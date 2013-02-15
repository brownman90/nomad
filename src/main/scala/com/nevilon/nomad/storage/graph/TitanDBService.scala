package com.nevilon.nomad.storage.graph

import org.apache.commons.configuration.{BaseConfiguration, Configuration}
import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
import scala.Predef._
import com.nevilon.nomad._
import com.tinkerpop.blueprints.{ThreadedTransactionalGraph, TransactionalGraph, Direction, Vertex}
import crawler.{Url, UrlStatus, Relation, Transformers}
import java.util.UUID
import collection.mutable
import com.nevilon.nomad.crawler.UrlStatus
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

  private val connector = new BerkeleyGraphStorageConnector
  private val graph = connector.getGraph

  def disconnect() {
    synchronized {
      connector.shutdown()
    }
  }


  def getOrCreateUrl(url: Url): Vertex = {
    synchronized {
      getUrl(url.location) match {
        case None => {
          //create url
          addOrUpdateUrl(url)
        }
        case Some(doc) => {
          doc
        }
      }
    }
  }

  private def getUrl(url: String): Option[Vertex] = {
    synchronized {
      // val tx = graph.startTransaction()
      val vertices = graph.getVertices("location", url)
      // tx.stopTransaction(Conclusion.SUCCESS)
      if (vertices.isEmpty) {
        None
      } else {
        if (vertices.size > 1) {
          throw new RuntimeException("By some really strange reasons there are more than one page with this url!")
        } else {
          Some(vertices.iterator.next())
        }
      }

    }


  }

  def linkUrls(relations: List[Relation]) {
    synchronized {
      // val tx = graph.startTransaction()

      relations.foreach(relation => {
        val parentPage = getOrCreateUrl(relation.from)
        val childPage = getOrCreateUrl(relation.to)
        graph.addEdge(UUID.randomUUID().toString, parentPage, childPage, "relation")
      })
    }
    graph.stopTransaction(Conclusion.SUCCESS)

  }


  def addOrUpdateUrl(url: Url): Vertex = {
    synchronized {
      //val tx = graph.startTransaction()
      val vertex = {
        getUrl(url.location) match {
          case None => {
            graph.addVertex()
          }
          case Some(v) => v
        }
      }

      vertex.setProperty("status", url.status.toString)
      //  graph.createKeyIndex("location", classOf[Vertex])

      vertex.setProperty("location", url.location)
      vertex.setProperty("fileId", url.fileId)
      //  graph.stopTransaction(Conclusion.SUCCESS)
      // graph.stopTransaction(Conclusion.SUCCESS)

      vertex
    }
  }


  def getBFSLinks(url: String, limit: Int): List[Url] = {
    synchronized {
      val rootVertex = getUrl(url).get //graph.getVertices("location",url).iterator()
      val traverser = new BFSTraverser(rootVertex, limit)
      traverser.traverse()
    }
  }

  class BFSTraverser(val startVertex: Vertex, val limit: Int) {

    private val closedSet = new mutable.HashSet[Vertex]
    private var queue = new mutable.Queue[Vertex]
    private val urls = new mutable.Queue[Url]

    //recursive

    /*
       node with NEW status - to urls
       node with any status  - to closedSet
       node with status=COMPLETE - to queue

       if status - complete - iterate over child
          add all new to query

     */
    //add v to closed set?

    def traverse(): List[Url] = {
      //verify()
      val tx = graph.startTransaction()
      val startUrl = Transformers.vertex2Url(startVertex)
      if (startUrl.status == UrlStatus.NEW) {
        urls += startUrl
      }
      queue += startVertex
      val depthLimit = 10 //TODO implement usage!!!
      while (queue.size > 0 && urls.size < limit) {
        val currentVertex = queue.front
        queue = queue.tail
        currentVertex.getVertices(Direction.OUT, "relation").iterator().foreach(v => {
          if (!(closedSet contains (v))) {
            closedSet += v
            val url = Transformers.vertex2Url(v)
            if (url.status == UrlStatus.NEW) {
              urls += url
            } else if (url.status == UrlStatus.COMPLETE) {
              queue += v
            }
          }
        })
      }
      tx.stopTransaction(Conclusion.SUCCESS)
      urls.toList
    }

  }


}