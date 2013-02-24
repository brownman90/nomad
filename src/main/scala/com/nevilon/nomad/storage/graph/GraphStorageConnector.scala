package com.nevilon.nomad.storage.graph

import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
import org.apache.commons.configuration.{BaseConfiguration, Configuration}
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion
import com.nevilon.nomad.boot.{GraphStorageConfig, InMemoryConfig, BerkeleyConfig, CassandraConfig}
import org.apache.cassandra.thrift.Cassandra
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.{TFramedTransport, TSocket}
import com.nevilon.nomad.logs.Logs

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/15/13
 * Time: 8:22 AM
 */

abstract class GraphStorageConnector(val conf: GraphStorageConfig) extends  Logs{

  protected val graph: TitanGraph = {
    if (conf.drop) drop()
    connect()
  }

  def getGraph = graph

  def shutdown()

  def drop()

  def connect(): TitanGraph

}

class CassandraGraphStorageConnector(conf: CassandraConfig) extends GraphStorageConnector(conf) {

  override def drop() {
    val transport = new TFramedTransport(new TSocket(conf.host, 9160))
    val protocol = new TBinaryProtocol(transport)
    val client = new Cassandra.Client(protocol)
    transport.open()
    client.system_drop_keyspace("titan") // "titan" is default keyspace
    info("keyspace is dropped")
    transport.close()
  }

  override def connect(): TitanGraph = {
    val titanConf: Configuration = new BaseConfiguration
    titanConf.setProperty("storage.backend", "cassandra")
    titanConf.setProperty("storage.hostname", conf.host)
    val graph = TitanFactory.open(titanConf)
    graph.createKeyIndex("location", classOf[Vertex])
    graph.stopTransaction(Conclusion.SUCCESS)
    graph
  }

  def shutdown() {
    graph.shutdown()
  }
}


class BerkeleyGraphStorageConnector(conf: BerkeleyConfig) extends GraphStorageConnector(conf) {

  override def drop() {}

  override def connect(): TitanGraph = {
    val titanConf: Configuration = new BaseConfiguration

    titanConf.setProperty("storage.directory", conf.directory)
    titanConf.setProperty("storage.backend", "berkeleyje")
    titanConf.setProperty("ids.flush", "true")
    titanConf.setProperty("storage.cache-percentage", 20)

    val graph = TitanFactory.open(titanConf)
    graph.createKeyIndex("location", classOf[Vertex])
    graph.stopTransaction(Conclusion.SUCCESS)
    graph
  }


  def shutdown() {
    graph.shutdown()
  }

}

class InMemoryGraphStorageConnector(conf: InMemoryConfig) extends GraphStorageConnector(conf) {

  override def drop() {}

  override def connect(): TitanGraph = {
    val graph =  TitanFactory.openInMemoryGraph()
    graph.createKeyIndex("location", classOf[Vertex])
    graph
  }


  def shutdown() {
    graph.shutdown()
  }

}
