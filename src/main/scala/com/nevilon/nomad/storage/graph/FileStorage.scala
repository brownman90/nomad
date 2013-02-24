package com.nevilon.nomad.storage.graph

import com.mongodb.casbah.commons.conversions.scala._
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.Imports._
import java.io.InputStream
import com.nevilon.nomad.boot.MongoDBConfig


/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/31/13
 * Time: 1:02 PM
 */

class FileStorage(conf:MongoDBConfig) {
  /*
     what about autoclean param on each start?
     check for file existence?

   */

  private var mongoClient: MongoClient = null
  private var gridfs: GridFS = null
  private var mongoDB: MongoDB = null

  RegisterJodaTimeConversionHelpers()

  connect()

  private def connect() {
    mongoClient = MongoClient(conf.host, conf.port)
    if(conf.drop){
      mongoClient.dropDatabase(conf.dbName)
    }
    mongoDB = mongoClient(conf.dbName)
    gridfs = GridFS(mongoDB)
  }

  def saveStream(is: InputStream, url: String, contentType: String): Option[String] = {
    gridfs(is) {
      file =>
        file.contentType = contentType
        file.filename = url
    }
    match {
      case None => None //throw exception or None?
      case Some(objectId) => {
        Some(objectId.asInstanceOf[ObjectId].toString)
      }
    }
  }


}
