package com.nevilon.nomad.storage.graph

import java.io.File
import org.apache.commons.configuration.{BaseConfiguration, Configuration}
import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
import scala.Predef._
import com.nevilon.nomad._
import com.tinkerpop.blueprints.{Direction, Vertex}
import java.util.UUID
import org.apache.commons.io.FileUtils
import collection.mutable
import com.nevilon.nomad.UrlStatus
import scala.Some

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/18/13
 * Time: 3:49 AM
 */
class TitanDBService(recreateDb: Boolean) {

  var graph: TitanGraph = null

  connect()


  def disconnect() {
    graph.shutdown()
  }

  def connect() {
    val path: String = "/tmp/dbstorage/"
    val dbDir: File = new File(path)

    if (recreateDb && dbDir.exists()) {
      FileUtils.deleteDirectory(dbDir)
    }
    var wasCreated = false
    if (!dbDir.exists) {
      dbDir.mkdir
      wasCreated = true
      /*
         create indexes and so on
       */
    }
    val conf: Configuration = new BaseConfiguration
    conf.setProperty("storage.directory", path)
    conf.setProperty("storage.backend", "berkeleyje")
    conf.setProperty("ids.flush", "true")
    graph = TitanFactory.openInMemoryGraph() //open(conf)
    if (wasCreated) {
      graph.createKeyIndex("location", classOf[Vertex])
    }
  }

  def getOrCreateUrl(url: String): Vertex = {
    getUrl(url) match {
      case None => {
        //create url
        addUrl(url)
      }
      case Some(doc) => {
        println("find already saved link")
        doc
      }
    }
  }

  def getUrl(url: String): Option[Vertex] = {
    //dumo

    val vertices = graph.getVertices("location", url)
    import scala.collection.JavaConversions._
    // println("vertices: " + graph.getVertices().size)


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

  def linkUrls(relations: List[RawUrlRelation]) {
    val lr = new RawUrlRelation("", "")

    import scala.collection.JavaConversions._
    println("vertices: " + graph.getVertices().size)
    relations.foreach(relation => {
      println(relation)
      val parentPage = getOrCreateUrl(relation.from)
      val childPage = getOrCreateUrl(relation.to)
      graph.addEdge(UUID.randomUUID().toString, parentPage, childPage, "relation")
    })
  }


  def addUrl(url: String): Vertex = {
    val vertex = graph.addVertex(UUID.randomUUID().toString)
    vertex.setProperty("location", url)
    vertex.setProperty("status", UrlStatus.New.toString())
    vertex
  }


  def updateUrlStatus(url: String, urlStatus: UrlStatus.Value) {
    getUrl(url) match {
      case None => throw new RuntimeException("Sorry, url not found!")
      case Some(vertex) => {
        vertex.setProperty("status", urlStatus)
      }
    }
  }

  def getBFSLinks(url: String, limit: Int): List[Url] = {
    val rootVertex = getUrl(url).get //graph.getVertices("location",url).iterator()
    //val vertIt =  rootVertex.query().vertices().has("status",UrlStatus.New.toString()).vertices().iterator()
    /*for (vertex <- rootVertex.getVertices(Direction.BOTH, "relation")) {
      println("item: " + vertex.getProperty("location"))
    }
    */
    val traverser = new BFSTraverser(rootVertex, 50)
    traverser.traverse()
    //new ListBuffer[Url].toList
    //graph.getVertices("","").iterator().next().query().
  }

  class BFSTraverser(val startVertex: Vertex, val limit: Int) {

    val closedSet = new mutable.HashSet[Vertex]
    var queue = new mutable.Queue[Vertex]
    val urls = new mutable.Queue[Url]


    //recursive
    def traverse(): List[Url] = {
      val startUrl = Transformers.vertex2Url(startVertex)
      if (startUrl.status == UrlStatus.New) {
        urls += startUrl
      }

      queue += startVertex
      val depthLimit = 10
      while (queue.size > 0 && urls.size < limit) {
        //correct?
        val currentVertex = queue.front
        queue = queue.tail
        //queue = tail!
        import scala.collection.JavaConversions._

        /*
           node with NEW status - to urls
           node with any status  - to closedSet
           node with status=COMPLETE - to queue

           if status - complete - iterate over child
              add all new to query

         */

        currentVertex.getVertices(Direction.OUT, "relation").iterator().foreach(v => {
          if (!(closedSet contains (v))) {
            val url = Transformers.vertex2Url(v)
            if (url.status == UrlStatus.New) {
              urls += url
            } else if (url.status == UrlStatus.Complete) {
              queue += v
            }
          }
        })
        closedSet += currentVertex
      }
      urls.toList
    }

  }


}
