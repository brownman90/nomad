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
package com.nevilon.nomad.storage.graph

import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
import org.apache.commons.configuration.{BaseConfiguration, Configuration}
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion
import com.nevilon.nomad.boot._
import org.apache.cassandra.thrift.Cassandra
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.{TFramedTransport, TSocket}
import com.nevilon.nomad.logs.Logs
import java.io.File
import org.apache.commons.io.FileUtils
import scala.collection.JavaConversions._


class ConnectorSelector extends Logs {

  private val conf = GlobalConfig

  private val connector =
    conf.titanConfig.backend match {
      case "cassandra" => new CassandraGraphStorageConnector(conf.cassandraConfig)
      case "inmemory" => new InMemoryGraphStorageConnector(conf.inMemoryConfig)
      case "berkeley" => new BerkeleyGraphStorageConnector(conf.berkeleyConfig)
      case _ => {
        error("wrong backend configuration")
        throw new Error
      }
    }

  def shutdown() {
    connector.shutdown()
  }

  lazy val graph = connector.getGraph

}


abstract class GraphStorageConnector(val conf: GraphStorageConfig) extends Logs {

  protected val graph: TitanGraph = synchronized {
    if (conf.drop) drop()
    createTypes(connect())
  }

  private def createTypes(graph: TitanGraph): TitanGraph = {
    graph.makeType().name(GraphProperties.Url.locationProperty).dataType(classOf[String]).indexed().unique().functional().makePropertyKey()
    graph.makeType().name(GraphProperties.Domain.nameProperty).dataType(classOf[String]).indexed().unique().functional().makePropertyKey()
    graph.stopTransaction(Conclusion.SUCCESS)
    graph
  }

  def shutdown() = synchronized {
    graph.shutdown()
  }

  def getGraph = graph

  def drop()

  def connect(): TitanGraph

}

class CassandraGraphStorageConnector(conf: CassandraConfig) extends GraphStorageConnector(conf) {


  override def drop() {
    val transport = new TFramedTransport(new TSocket(conf.host, 9160))
    val protocol = new TBinaryProtocol(transport)
    val client = new Cassandra.Client(protocol)
    transport.open()

    client.describe_keyspaces().find(ksDef => ksDef.name == conf.keyspace) match {
      case None => info("keyspace not found")
      case _ => {
        client.system_drop_keyspace(conf.keyspace)
        info("keyspace is dropped")
      }
    }
    transport.close()
  }

  override def connect(): TitanGraph = {
    val titanConf: Configuration = new BaseConfiguration
    titanConf.setProperty("storage.backend", "cassandra")
    titanConf.setProperty("storage.hostname", conf.host)
    titanConf.setProperty("storage.keyspace", conf.keyspace)
    val graph = TitanFactory.open(titanConf)
    graph
  }

}


class BerkeleyGraphStorageConnector(conf: BerkeleyConfig) extends GraphStorageConnector(conf) {


  override def drop() {
    val dbDir = new File(conf.directory)
    if (dbDir.exists()) {
      FileUtils.deleteDirectory(dbDir)
    }
  }

  override def connect(): TitanGraph = {
    val dbDir = new File(conf.directory)
    if (!dbDir.exists()) {
      dbDir.mkdirs
    }

    val titanConf: Configuration = new BaseConfiguration

    titanConf.setProperty("storage.directory", conf.directory)
    titanConf.setProperty("storage.backend", "berkeleyje")
    titanConf.setProperty("ids.flush", "true")
    titanConf.setProperty("storage.cache-percentage", 20)

    val graph = TitanFactory.open(titanConf)
    graph.stopTransaction(Conclusion.SUCCESS)
    graph
  }


}

class InMemoryGraphStorageConnector(conf: InMemoryConfig) extends GraphStorageConnector(conf) {

  override def drop() {}

  override def connect(): TitanGraph = synchronized {
    TitanFactory.openInMemoryGraph()
  }


}
