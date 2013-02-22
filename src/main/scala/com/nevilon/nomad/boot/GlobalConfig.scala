package com.nevilon.nomad.boot

import com.typesafe.config.ConfigFactory

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/22/13
 * Time: 2:11 PM
 */
object GlobalConfig {

  private val conf = ConfigFactory.load()
  conf.checkValid(ConfigFactory.defaultReference())

  val inMemoryConfig: InMemoryConfig = new InMemoryConfig {

  }

  val cassandraConfig: CassandraConfig = new CassandraConfig {
    def host(): String = conf.getString("storage.titan.backends.cassandra.host")
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

trait InMemoryConfig {}

trait BerkeleyConfig {

  def directory: String

  def drop: Boolean

}

trait CassandraConfig {

  def host: String

}

trait TitanConfig {

  def backend: String

}
