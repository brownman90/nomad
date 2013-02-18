package com.nevilon.nomad.storage.graph

import com.tinkerpop.blueprints.{Direction, Vertex}
import collection.mutable
import com.nevilon.nomad.crawler.{UrlStatus, Transformers, Url}
import scala.collection.JavaConversions._


/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/15/13
 * Time: 9:16 AM
 */

class BFSTraverser(val startVertex: Vertex, val limit: Int) {

  private val closedSet = new mutable.HashSet[String]
  private var queue = new mutable.Queue[Vertex]
  private val urls = new mutable.Queue[Url]

  //recursive

  /*
     node with NEW status - to urls
     node with any status  - to closedSet
     node with status=COMPLETE - to queue

     if status - complete - iterate over child
        add all new to query

   */
  def traverse(): List[Url] = {
    //verify()
    //val tx = graph.startTransaction()
    val startUrl = Transformers.vertex2Url(startVertex)
    if (startUrl.status == UrlStatus.NEW) {
      urls += startUrl
    }
    queue += startVertex
    val depthLimit = 50 //TODO implement usage!!!

    while (queue.size > 0 && urls.size < limit) {
      val currentVertex = queue.front
      queue = queue.tail
      currentVertex.getVertices(Direction.OUT, "relation").iterator().foreach(v => {
        val url = Transformers.vertex2Url(v)
        if (!(closedSet contains (url.location))) {
          // closedSet += v
          if (url.status == UrlStatus.NEW || !urls.contains(url)) {
            urls += url //duplicates?
          } else if (url.status == UrlStatus.COMPLETE) {
            queue += v
          }
        }

      })
      closedSet += Transformers.vertex2Url(currentVertex).location
    }
    // tx.stopTransaction(Conclusion.SUCCESS)
    //require(urls.toList.size == urls.toList.distinct.size)
    urls.toList
  }

}