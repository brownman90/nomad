package com.nevilon.nomad.boot

import com.typesafe.config.{Config, ConfigFactory}
import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/22/13
 * Time: 2:11 PM
 */


object GlobalConfig {

  var profile: Profile = null

  private var conf: Config = null

  def load(configFile: File) {
    //we need this line because some geniuses think that we will load file only from CLASSPATH
    // or file specified by -Dconfig.file
    System.setProperty("config.file", configFile.getAbsolutePath)
    conf = ConfigFactory.load
    conf.checkValid(ConfigFactory.defaultReference())
  }


  val inMemoryConfig: InMemoryConfig = new InMemoryConfig {
    def drop = false
  }

  val cassandraConfig: CassandraConfig = new CassandraConfig {
    def host(): String = conf.getString("storage.titan.backends.cassandra.host")

    def drop(): Boolean = conf.getBoolean("storage.titan.backends.cassandra.drop")

  }

  val berkeleyConfig: BerkeleyConfig = new BerkeleyConfig {
    def directory(): String = conf.getString("storage.titan.backends.berkeley.directory")

    def drop(): Boolean = conf.getBoolean("storage.titan.backends.berkeley.drop")
  }

  val titanConfig: TitanConfig = new TitanConfig {

    def backend(): String = conf.getString("storage.titan.main_connector")
  }

  val mongoDBConfig: MongoDBConfig = new MongoDBConfig {

    def dbName(): String = conf.getString("storage.mongo.db_name")

    def host(): String = conf.getString("storage.mongo.host")

    def drop(): Boolean = conf.getBoolean("storage.mongo.drop")

    def port(): Int = conf.getInt("storage.mongo.port")
  }


  val linksConfig: LinksConfig = new LinksConfig {
    def bfsLimit(): Int = conf.getInt("links.bfs_limit")

    def extractedLinksCache(): Int = conf.getInt("links.extracted_links_cache")
  }


  val masterConfig: MasterConfig = new MasterConfig {
    def threadsInWorker(): Int = conf.getInt("master.threads_in_worker")

    def workers(): Int = conf.getInt("master.workers")
  }

  val appConfig: AppConfig = new AppConfig {
    def defaultSeedFile(): String = conf.getString("app.default_seed")
  }


}

trait AppConfig {

  def defaultSeedFile: String

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

}

trait TitanConfig {

  def backend: String

}


trait ConfigProvider {

  val config = GlobalConfig

}
