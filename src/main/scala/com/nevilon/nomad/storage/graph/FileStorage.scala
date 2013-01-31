package com.nevilon.nomad.storage.graph

import com.mongodb.casbah.commons.conversions.scala._
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.gridfs.Imports._
import java.io.{InputStream, FileInputStream}


/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/31/13
 * Time: 1:02 PM
 */

class FileStorage {
  /*
     what about autoclean param on each start?

   */
  private val DB_NAME = "nomad"
  private val DB_HOST = "localhost"
  private val DB_PORT = 27017

  private var mongoClient: MongoClient = null
  private var gridfs: GridFS = null
  private var mongoDB: MongoDB = null

  RegisterJodaTimeConversionHelpers()

  connect()

  private def connect() {
    mongoClient = MongoClient(DB_HOST, DB_PORT)
    mongoDB = mongoClient(DB_NAME)
    gridfs = GridFS(mongoDB)
  }

  def saveStream(is: InputStream, url: String, contentType: String) {
    gridfs(is) {
      file =>
        file.contentType = contentType
        file.filename = url
    }
  }


}
