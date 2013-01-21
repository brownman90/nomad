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
import org.eclipse.jdt.internal.core.Assert

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
    //graph = TitanFactory.open(conf)
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
        doc
      }
    }
  }

  def getUrl(url: String): Option[Vertex] = {
    val vertices = graph.getVertices("location", url)
    import scala.collection.JavaConversions._

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
    relations.foreach(relation => {
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
    // this.synchronized{
    getUrl(url) match {
      case None => throw new RuntimeException("Sorry, url not found!")
      case Some(vertex) => {
        Assert.isNotNull(urlStatus)
        vertex.setProperty("status", urlStatus)
      }
    }
    //  }

  }

  def getBFSLinks(url: String, limit: Int): List[Url] = {
    val rootVertex = getUrl(url).get //graph.getVertices("location",url).iterator()
    val traverser = new BFSTraverser(rootVertex, 50)
    traverser.traverse()
  }

  class BFSTraverser(val startVertex: Vertex, val limit: Int) {

    val closedSet = new mutable.HashSet[Vertex]
    var queue = new mutable.Queue[Vertex]
    val urls = new mutable.Queue[Url]


    //recursive
    def traverse(): List[Url] = {
      val startUrl = Transformers.vertex2Url(startVertex).get //sorry, but statusUrl SHOULD exists at this moment!

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
            url match {
              case Some(urlObj) => {
                if (urlObj.status == UrlStatus.New) {
                  urls += urlObj
                } else if (urlObj.status == UrlStatus.Complete) {
                  queue += v
                }
              }
               //this is workaround. Sorry. See comment at transformer
              case None => {
                if (v.getVertices(Direction.OUT,"relation").size>0){
                  //mark as complete // or in progress???
                  v.setProperty("status", UrlStatus.Complete.toString)
                  queue += v
                } else {
                  //no child, so this is our guy
                  //mark as new
                  v.setProperty("status", UrlStatus.New.toString)
                  //process
                  urls += Transformers.vertex2Url(v).get
                }
                throw  new Error("fail")

              }
            }


          }
        })
        closedSet += currentVertex
      }
      urls.toList
    }

  }


}
