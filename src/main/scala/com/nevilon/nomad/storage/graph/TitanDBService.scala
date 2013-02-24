package com.nevilon.nomad.storage.graph

import scala.Predef._
import com.nevilon.nomad._
import boot.GlobalConfig
import com.tinkerpop.blueprints.Vertex
import crawler.{Url, Relation}
import java.util.UUID
import logs.Logs
import scala.Some
import scala.collection.JavaConversions._
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion
import com.thinkaurelius.titan.core.{TitanTransaction}
import java.lang.String
import scala.Predef.String

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/18/13
 * Time: 3:49 AM
 */


trait TitanDBService extends Logs {

  private val connector =
    GlobalConfig.titanConfig.backend match {
      case "cassandra" => new CassandraGraphStorageConnector(GlobalConfig.cassandraConfig)
      case "inmemory" => new InMemoryGraphStorageConnector(GlobalConfig.inMemoryConfig)
      case "berkeley" => new BerkeleyGraphStorageConnector(GlobalConfig.berkeleyConfig)
      case _ => {
        error("wrong backend configuration")
        throw new Error
      }
    }
  private val graph = connector.getGraph

  def shutdown() {
    synchronized {
      connector.shutdown()
    }
  }


  def linkUrls(relations: List[Relation]) {
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
        })

      }
    }
  }


  def saveOrUpdateUrl(url: Url) = {
    withTransaction {
      implicit tx => {
        saveOrUpdateUrlInTx(url)
      }
    }
  }


  def getBFSLinks(url: String, limit: Int): List[Url] = {
    withTransaction[List[Url]] {
      implicit tx => {
        val rootVertex = getUrlInTx(url).get
        new BFSTraverser(rootVertex, limit).traverse()
      }
    }
  }

  def getUrl(url: String): Option[Vertex] = {
    withTransaction[Option[Vertex]] {
      implicit tx =>
        getUrlInTx(url)
    }
  }

  private def saveOrUpdateUrlInTx(url: Url)(implicit tx: TitanTransaction): Vertex = {
    val vertex = {
      getUrlInTx(url.location) match {
        case None => tx.addVertex()
        case Some(v) => v
      }
    }

    vertex.setProperty("status", url.status.toString)
    vertex.setProperty("location", url.location)
    vertex.setProperty("fileId", url.fileId)

    vertex
  }

  private def getUrlInTx(url: String)(implicit tx: TitanTransaction): Option[Vertex] = {
    val vertices = tx.getVertices("location", url)
    if (vertices.isEmpty) None
    else if (vertices.size > 1)
      throw new RuntimeException("There are more than one page with this url!")
    else Some(vertices.iterator.next())

  }


  private def withTransaction[T](f: TitanTransaction => T): T = {
    val tx = graph.startTransaction()
    try {
      val result = f(tx)
      tx.stopTransaction(Conclusion.SUCCESS)
      result
    }
    catch {
      case ex: Throwable => tx.abort(); throw ex
    }
  }


}