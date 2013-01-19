package com.nevillon.nomad

import org.junit.{Assert, Test}
import com.nevilon.nomad.storage.graph.TitanDBService
import collection.mutable.ListBuffer
import com.nevilon.nomad.Types
import com.tinkerpop.blueprints.Direction

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/18/13
 * Time: 6:07 AM
 */
class TitanDBServiceTest {


  @Test def insertAndSelectTest() {
    val service = new TitanDBService(true)
    val url_1 = service.addUrl("http://lenta.ru")
    service.getUrl("http://lenta.ru") match {
      case None => {
        Assert.fail("not found")
      }
      case Some(vertex) => Assert.assertEquals(url_1.getProperty("location"), vertex.getProperty("location"))
    }
    service.disconnect()
  }

  @Test def makeLinkTest() {
    val service = new TitanDBService(true)
    //add some links
    val url_0 = service.addUrl("http://lenta.ru/")
    val url_1 = service.addUrl("http://lenta.ru/1")
    val url_2 = service.addUrl("http://lenta.ru/2")
    val url_3 = service.addUrl("http://lenta.ru/3")
    //make connections
    val relations = new ListBuffer[Types.LinkRelation]
    relations += (("http://lenta.ru/", "http://lenta.ru/1"))
    relations += (("http://lenta.ru/", "http://lenta.ru/2"))
    relations += (("http://lenta.ru/2", "http://lenta.ru/3"))
    //linkify
    service.linkUrls(relations.toList)
    //check if created!
    //storage for extracted relations
    var extractedRelations = new ListBuffer[Types.LinkRelation]
    //
    service.getUrl("http://lenta.ru/") match {
      case None => Assert.fail("not found")
      case Some(vertex) => {
        import scala.collection.JavaConversions._
        val it = vertex.getVertices(Direction.OUT, "relation").iterator()
        for (v <- it) {
          extractedRelations += (("http://lenta.ru/", v.getProperty("location").toString))
        }
      }
    }
    //
    service.getUrl("http://lenta.ru/2") match {
      case None => Assert.fail("not found")
      case Some(vertex) => {
        import scala.collection.JavaConversions._
        val it = vertex.getVertices(Direction.OUT, "relation").iterator()
        for (v <- it) {
          extractedRelations += (("http://lenta.ru/2", v.getProperty("location").toString))
        }
      }
    }
    Assert.assertEquals(relations,extractedRelations)
    //close
    service.disconnect()
  }


  @Test def traverseTest() {
    val service = new TitanDBService(true)
    //add some links
    val url_0 = service.addUrl("http://lenta.ru/")
    val url_1 = service.addUrl("http://lenta.ru/1")
    val url_2 = service.addUrl("http://lenta.ru/2")
    val url_3 = service.addUrl("http://lenta.ru/3")
    val url_4 = service.addUrl("http://lenta.ru/4")
    val url_5 = service.addUrl("http://lenta.ru/5")

    //make connections
    val relations = new ListBuffer[Types.LinkRelation]
    relations += (("http://lenta.ru/", "http://lenta.ru/1"))
    relations += (("http://lenta.ru/", "http://lenta.ru/5"))
    relations += (("http://lenta.ru/", "http://lenta.ru/2"))
    relations += (("http://lenta.ru/2", "http://lenta.ru/3"))
    relations += (("http://lenta.ru/2", "http://lenta.ru/4"))
    //linkify
    service.linkUrls(relations.toList)
    //traverse
    val bfsLinks = service.getBFSLinks("http://lenta.ru/", 1000)
    println("bfs links:" + bfsLinks.length)
    //close
    service.disconnect()
  }


  @Test def countTest(){
    val service = new TitanDBService(false)
    import scala.collection.JavaConversions._
    println( service.graph.getVertices().size)
  }

  @Test def getOrCreateTest() {

  }


}
