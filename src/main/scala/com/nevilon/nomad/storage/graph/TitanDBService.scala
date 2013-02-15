package com.nevilon.nomad.storage.graph

import java.io.File
import org.apache.commons.configuration.{BaseConfiguration, Configuration}
import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
import scala.Predef._
import com.nevilon.nomad._
import com.tinkerpop.blueprints.{ThreadedTransactionalGraph, TransactionalGraph, Direction, Vertex}
import crawler.{Url, UrlStatus, Relation, Transformers}
import filter.Action
import java.util.UUID
import org.apache.commons.io.FileUtils
import collection.mutable
import com.nevilon.nomad.crawler.UrlStatus
import scala.Some
import org.eclipse.jdt.internal.core.Assert
import org.apache.log4j.LogManager
import scala.collection.JavaConversions._
import collection.mutable.ListBuffer
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/18/13
 * Time: 3:49 AM
 */

trait GraphStorage {

  def getGraph(): TitanGraph

  def shutdown()

}

class CassandraGraphStorage extends GraphStorage {

  private val graph = {
    val conf: Configuration = new BaseConfiguration
    conf.setProperty("storage.backend", "cassandra")
    conf.setProperty("storage.hostname", "127.0.0.1")
    val graph = TitanFactory.open(conf)
    graph.createKeyIndex("location", classOf[Vertex])
    graph.stopTransaction(Conclusion.SUCCESS)
    graph
  }

  def getGraph() = graph

  def shutdown() {
    graph.shutdown()
  }
}


class BerkeleyGraphStorage extends GraphStorage {

  private val graph = {

    val path: String = "/tmp/berk/"
    val conf: Configuration = new BaseConfiguration

    conf.setProperty("storage.directory", path)
    conf.setProperty("buffer-size", "1000")
    conf.setProperty("storage.backend", "berkeleyje")
    conf.setProperty("ids.flush", "true")
    conf.setProperty("storage.cache-percentage", 1)
    conf.setProperty("storage.transactions", "false")

    val graph = TitanFactory.open(conf)
    graph.createKeyIndex("location", classOf[Vertex])
    graph.stopTransaction(Conclusion.SUCCESS)
    graph
  }

  def getGraph() = graph

  def shutdown() {
    graph.shutdown()
  }

}

class InMemoryGraphStorage extends GraphStorage {

  private val graph = TitanFactory.openInMemoryGraph()

  def getGraph() = graph

  def shutdown() {
    graph.shutdown()
  }

}


class TitanDBService(recreateDb: Boolean) {

  private var graph: TitanGraph = null

  connect()

  def disconnect() {
    synchronized {
      graph.shutdown()
    }
  }

  private def connect() {
    synchronized {
      val conf: Configuration = new BaseConfiguration
      conf.setProperty("storage.backend", "cassandra")
      conf.setProperty("storage.hostname", "127.0.0.1")

      graph = TitanFactory.open(conf)
      graph.createKeyIndex("location", classOf[Vertex])
      graph.stopTransaction(Conclusion.SUCCESS)
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