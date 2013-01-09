package com.nevilon.nomad

import com.orientechnologies.orient.core.db.graph.OGraphDatabase
import com.tinkerpop.blueprints.impls.orient.OrientGraph
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import com.orientechnologies.orient.core.record.impl.ODocument
import com.tinkerpop.blueprints.Direction

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/9/13
 * Time: 11:31 AM
 * To change this template use File | Settings | File Templates.
 */
object OrientDBConnector {

  def main(args: Array[String]) {

    var graph: OrientGraph = null
    try {
      graph = new OrientGraph("remote:localhost/nomad", "admin", "admin")
      graph.getRawGraph().setUseCustomTypes(true)



     //  graph.getRawGraph().createVertexType("Page")
      val from = graph.addVertex("class:Page")
      from.setProperty("url", "www.lenta.ru")
      val to = graph.addVertex("class:Page")
      to.setProperty("url", "www.lenta.ru/news")
      val link = graph.addEdge(null, from, to, "knows")
      graph.stopTransaction(Conclusion.SUCCESS)

     // graph.getRawGraph.setRoot()

      //val it =  from.getEdges(Direction.OUT)
      //println(it.iterator().hasNext)

      val result = graph.getRawGraph().query[java.util.List[ODocument]](new OSQLSynchQuery[ODocument]("select from page where url='www.lenta.ru'"))

      println(result.size())
    } finally {
      if (graph != null)
        graph.shutdown()
    }


  }

}
