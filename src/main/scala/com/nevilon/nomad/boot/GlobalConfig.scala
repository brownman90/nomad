/**
 * Copyright (C) 2012-2013 Vadim Bartko (vadim.bartko@nevilon.com).
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * See file LICENSE.txt for License information.
 */
package com.nevilon.nomad.boot

import com.typesafe.config.{Config, ConfigFactory}
import java.io.File

object GlobalConfig {

  private var conf: Config = null

  private var absProfileDir: String = null

  private def getFullPath(filename: String): String = absProfileDir + "/" + filename

  def loadProfile(profileDir: String) {
    absProfileDir = profileDir
    val appConfFullPath = getFullPath("application.conf")
    if (!new File(appConfFullPath).exists()) {
      throw new Error("cannot load application.conf from " + appConfFullPath)
    }
    //we need this line because some geniuses think that we will load file only from CLASSPATH
    // or file specified by -Dconfig.file
    System.setProperty("config.file", appConfFullPath)
    conf = ConfigFactory.load
    conf.checkValid(ConfigFactory.defaultReference())
  }


  val inMemoryConfig: InMemoryConfig = new InMemoryConfig {

    def drop = false
  }

  val cassandraConfig: CassandraConfig = new CassandraConfig {
    def host = conf.getString("storage.titan.backends.cassandra.host")

    def drop = conf.getBoolean("storage.titan.backends.cassandra.drop")

    def keyspace = conf.getString("storage.titan.backends.cassandra.keyspace")
  }

  val berkeleyConfig: BerkeleyConfig = new BerkeleyConfig {
    def directory = conf.getString("storage.titan.backends.berkeley.directory")

    def drop = conf.getBoolean("storage.titan.backends.berkeley.drop")
  }

  val titanConfig: TitanConfig = new TitanConfig {

    def backend = conf.getString("storage.titan.main_connector")
  }

  val mongoDBConfig: MongoDBConfig = new MongoDBConfig {

    def dbName = conf.getString("storage.mongo.db_name")

    def host = conf.getString("storage.mongo.host")

    def drop = conf.getBoolean("storage.mongo.drop")

    def port = conf.getInt("storage.mongo.port")
  }


  val linksConfig: LinksConfig = new LinksConfig {
    def bfsLimit = conf.getInt("links.bfs_limit")

    def extractedLinksCache = conf.getInt("links.extracted_links_cache")
  }


  val masterConfig: MasterConfig = new MasterConfig {
    def threadsInWorker = conf.getInt("master.threads_in_worker")

    def workers = conf.getInt("master.workers")
  }

  val appConfig: AppConfig = new AppConfig {
    def seedFile = getFullPath(conf.getString("app.seed_file"))

    def filtersFile = getFullPath(conf.getString("app.filters_file"))

    def userAgent = conf.getString("app.user_agent")
  }


}

trait AppConfig {

  def seedFile: String

  def filtersFile: String

  def userAgent: String

}

trait MasterConfig {

  def workers: Int

  def threadsInWorker: Int

}


trait LinksConfig {

  def bfsLimit: Int

  def extractedLinksCache: Int

}

trait MongoDBConfig {

  def host: String

  def port: Int

  def dbName: String

  def drop: Boolean


}

trait GraphStorageConfig {

  def drop: Boolean
}

trait InMemoryConfig extends GraphStorageConfig {}

trait BerkeleyConfig extends GraphStorageConfig {

  def directory: String

}

trait CassandraConfig extends GraphStorageConfig {

  def host: String

  def keyspace: String

}

trait TitanConfig {

  def backend: String

}


trait ConfigProvider {

  val config = GlobalConfig

}
