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

  def traverse(): List[Url] = {
    val startUrl = Transformers.vertex2Url(startVertex)
    if (startUrl.status == UrlStatus.NEW) {
      urls += startUrl
    }
    queue += startVertex


    while (queue.size > 0 && urls.size < limit) {
      val currentVertex = queue.front
      queue = queue.tail
      println("queue " + queue.size + " closed set " + closedSet.size +
        " urls " + urls.size + " " + currentVertex.getProperty("location"))

      currentVertex.getVertices(Direction.OUT, "relation").iterator().foreach(v => {
        val url = Transformers.vertex2Url(v)
        if (  !queue.contains(v)  &&  !(closedSet contains (url.location))) {


         // println(url.status)
          if (url.status == UrlStatus.NEW &&  !urls.contains(url)) {
            urls += url
          } else if (url.status == UrlStatus.COMPLETE && !urls.contains(url)) {
          //  println("add to q")
            queue += v
          }
        }

      })
      closedSet += Transformers.vertex2Url(currentVertex).location
    }




    require(urls.toList.size == urls.toList.distinct.size)
    urls.toList
  }

}