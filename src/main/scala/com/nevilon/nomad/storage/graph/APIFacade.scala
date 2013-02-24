package com.nevilon.nomad.storage.graph

import com.mongodb.casbah.Imports._
import collection.mutable.ListBuffer
import org.joda.time.DateTime
import java.io.InputStream
import java.nio.file.{Files, FileSystems}
import com.nevilon.nomad.crawler.Transformers
import java.lang.reflect.Field


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


//  test()
//
//  def test() {
//    val entities = findAllPdfFiles()
//    println(entities.size)
//
//    entities.foreach(entity => {
//      println(entity)
//      // println(entity.url + " " + entity.contentType + Transformers.vertex2Url(getUrl(entity.url).get).status)
//
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


  def getIncoming() {

  }

  def getOutgoing() {

  }

}


class Entity(val size: Long, val url: String,
             val timestamp: DateTime, val id: String,
             val contentType: String, val md5: String, val urlId: String) extends StringGenerator


