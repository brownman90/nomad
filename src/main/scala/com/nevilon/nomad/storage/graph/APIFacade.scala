package com.nevilon.nomad.storage.graph

import com.mongodb.casbah.Imports._
import collection.mutable.ListBuffer
import org.joda.time.DateTime
import java.io.InputStream
import com.nevilon.nomad.crawler.{Url, Transformers}
import com.nevilon.nomad.utils.StringGenerator
import com.tinkerpop.blueprints.Direction

import scala.collection.JavaConversions._
import java.nio.file.{FileSystems, Files}
import com.nevilon.nomad.boot.GlobalConfig


/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/24/13
 * Time: 5:48 AM
 */
/*
We do not track files with the same hash here!

 */
class APIFacade extends TitanDBService with FileStorage {


//  def test() {
//    val entities = findAllPdfFiles()
//    entities.foreach(entity => {
//      getIncoming(entity.url).foreach(u => println(u.location))
//      getOutgoing(entity.url).foreach(t => println(t.location))
//      println(entity)
//      // println(entity.url + " " + entity.contentType + Transformers.vertex2Url(getUrl(entity.url).get).status)
//      val path = FileSystems.getDefault().getPath("/tmp/pdfs/", System.currentTimeMillis().toString + ".pdf");
//      Files.copy(getFileStream(entity.id), path)
//    })
//  }


  /*
    contentType
    domain
   */


  private implicit def DBObject2Entity(dbObject: DBObject) = {
    new Entity(
      dbObject.getAs[Long]("length").get,
      dbObject.getAs[String]("filename").get,
      new DateTime(dbObject.getAs[java.util.Date]("uploadDate").get.getTime),
      dbObject.getAs[ObjectId]("_id").get.toString,
      dbObject.getAs[String]("contentType").get,
      dbObject.getAs[String]("md5").get,
      dbObject.getAs[String]("urlId").get
    )
  }

  def findAllPdfFiles(): List[Entity] = {
    val entities = new ListBuffer[Entity]
    val q = ("length" $gt 100000) ++ ("contentType" -> "application/pdf")
    //val q = ("length" $gt 1)

    val result = getGridFS().files(q)
    result.foreach(obj => entities += obj)
    entities.toList
  }

  def getFileStream(fileId: String): InputStream = {
    getGridFS().findOne(new ObjectId(fileId)) match {
      case None => throw new RuntimeException("wrong fileId")
      case Some(gridFsFile) => gridFsFile.inputStream

    }
  }


  private def getConnectedUrls(url: String, direction: Direction): List[Url] = {
    getUrl(url) match {
      case None => List[Url]()
      case Some(v) => {
        val incoming = v.getVertices(direction, "relation").map(v => {
          Transformers.vertex2Url(v)
        })
        incoming.toList
      }
    }
  }

  def getIncoming(url: String): List[Url] = {
    getConnectedUrls(url, Direction.IN)
  }

  def getOutgoing(url: String): List[Url] = {
    getConnectedUrls(url, Direction.OUT)
  }


}


class Entity(val size: Long, val url: String,
             val timestamp: DateTime, val id: String,
             val contentType: String, val md5: String, val urlId: String) extends StringGenerator


