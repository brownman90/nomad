package com.nevillon.nomad

import org.junit.{Assert, Test}
import com.nevilon.nomad.storage.graph.TitanDBService
import collection.mutable.ListBuffer
import com.tinkerpop.blueprints.Direction
import com.nevilon.nomad.crawler.RawUrlRelation
import com.nevilon.nomad.filter.Action

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
    val relations = new ListBuffer[RawUrlRelation]
    relations += (new RawUrlRelation("http://lenta.ru/", "http://lenta.ru/1",Action.None))
    relations += (new RawUrlRelation("http://lenta.ru/", "http://lenta.ru/2",Action.None))
    relations += (new RawUrlRelation("http://lenta.ru/2", "http://lenta.ru/3",Action.None))
    //linkify
    service.linkUrls(relations.toList)
    //check if created!
    //storage for extracted relations
    var extractedRelations = new ListBuffer[RawUrlRelation]
    //
    service.getUrl("http://lenta.ru/") match {
      case None => Assert.fail("not found")
      case Some(vertex) => {
        import scala.collection.JavaConversions._
        val it = vertex.getVertices(Direction.OUT, "relation").iterator()
        for (v <- it) {
          extractedRelations += (new RawUrlRelation("http://lenta.ru/", v.getProperty("location").toString,Action.None))
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
          extractedRelations += (new RawUrlRelation("http://lenta.ru/2", v.getProperty("location").toString,Action.None))
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
    val relations = new ListBuffer[RawUrlRelation]
    relations += (new RawUrlRelation("http://lenta.ru/", "http://lenta.ru/1",Action.None))
    relations += (new RawUrlRelation("http://lenta.ru/", "http://lenta.ru/5",Action.None))
    relations += (new RawUrlRelation("http://lenta.ru/", "http://lenta.ru/2",Action.None))
    relations += (new RawUrlRelation("http://lenta.ru/2", "http://lenta.ru/3",Action.None))
    relations += (new RawUrlRelation("http://lenta.ru/2", "http://lenta.ru/4",Action.None))
    //linkify
    service.linkUrls(relations.toList)
    //traverse
    val bfsLinks = service.getBFSLinks("http://lenta.ru/", 1000)
    println("bfs links:" + bfsLinks.length)
    //close
    service.disconnect()
  }


  @Test def groupDuplicatesTest(){
    val relations = new ListBuffer[RawUrlRelation]
    relations += (new RawUrlRelation("http://lenta.ru/", "http://lenta.ru/1",Action.None))
    relations += (new RawUrlRelation("http://lenta.ru/", "http://lenta.ru/1",Action.None))
    relations += (new RawUrlRelation("http://lenta.ru/", "http://lenta.ru/5",Action.None))
    relations += (new RawUrlRelation("http://lenta.ru/", "http://lenta.ru/2",Action.None))
    relations += (new RawUrlRelation("http://lenta.ru/2", "http://lenta.ru/3",Action.None))
    relations += (new RawUrlRelation("http://lenta.ru/2", "http://lenta.ru/4",Action.None))
    val counts = relations.groupBy(w => w.to).mapValues(_.size)
    val distincts = relations.distinct
    val first = new RawUrlRelation("http://lenta.ru/", "http://lenta.ru/1",Action.None)
    val second = new RawUrlRelation("http://lenta.ru/", "http://lenta.ru/1",Action.None)
  }



}
