package com.nevilon.nomad.storage.graph

import com.thinkaurelius.titan.core.{TitanFactory, TitanGraph}
import org.apache.commons.configuration.{BaseConfiguration, Configuration}
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion

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

class CassandraGraphStorageConnector extends GraphStorageConnector {

  protected val graph = {
    val conf: Configuration = new BaseConfiguration
    conf.setProperty("storage.backend", "cassandra")
    conf.setProperty("storage.hostname", "127.0.0.1")
    val graph = TitanFactory.open(conf)
    graph.createKeyIndex("location", classOf[Vertex])
    graph.stopTransaction(Conclusion.SUCCESS)
    graph
  }

  def shutdown() {
    graph.shutdown()
  }
}


class BerkeleyGraphStorageConnector extends GraphStorageConnector {

  protected val graph = {

    val path: String = "/tmp/b"
    val conf: Configuration = new BaseConfiguration

    conf.setProperty("storage.directory", path)
    conf.setProperty("storage.backend", "berkeleyje")
    conf.setProperty("ids.flush", "true")
    conf.setProperty("storage.cache-percentage", 20)

    val graph = TitanFactory.open(conf)
    graph.createKeyIndex("location", classOf[Vertex])
    graph.stopTransaction(Conclusion.SUCCESS)
    graph
  }

  def shutdown() {
    graph.shutdown()
  }

}

class InMemoryGraphStorageConnector extends GraphStorageConnector {

  protected val graph = TitanFactory.openInMemoryGraph()
  graph.createKeyIndex("location", classOf[Vertex])


  def shutdown() {
    graph.shutdown()
  }

}
