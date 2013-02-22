package com.nevilon.nomad.storage.graph

import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
import org.apache.commons.configuration.{BaseConfiguration, Configuration}
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion
import com.nevilon.nomad.boot.{InMemoryConfig, BerkeleyConfig, CassandraConfig}

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/15/13
 * Time: 8:22 AM
 */

abstract class GraphStorageConnector {

  protected val graph: TitanGraph

  def getGraph = graph

  def shutdown()

}

class CassandraGraphStorageConnector(conf: CassandraConfig) extends GraphStorageConnector {

  protected val graph = {
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


class BerkeleyGraphStorageConnector(conf:BerkeleyConfig) extends GraphStorageConnector {

  protected val graph = {

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

class InMemoryGraphStorageConnector(conf:InMemoryConfig) extends GraphStorageConnector {

  protected val graph = TitanFactory.openInMemoryGraph()
  graph.createKeyIndex("location", classOf[Vertex])


  def shutdown() {
    graph.shutdown()
  }

}
