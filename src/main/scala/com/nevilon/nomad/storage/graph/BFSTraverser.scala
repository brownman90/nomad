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

import com.tinkerpop.blueprints.{Direction, Vertex}
import collection.mutable
import com.nevilon.nomad.crawler.{UrlStatus, Transformers, Url}
import scala.collection.JavaConversions._



class BFSTraverser(val startVertex: Vertex, val limit: Int) {

  private val closedSet = new mutable.HashSet[String]
  private var queue = new mutable.HashSet[Vertex]  // but we need queue for BFS!!!
  private val urls = new mutable.HashSet[Url]


  implicit def Vertex2Url(vertex: Vertex) = Transformers.vertex2Url(vertex)

  def traverse(): List[Url] = {
    if (startVertex.status == UrlStatus.NEW) urls += startVertex
    queue += startVertex

    while (queue.size > 0 && urls.size < limit) {
     // println(queue.size + " " + closedSet.size + " urls " + urls.size)
      queue.toList match {
        case ::(head, tail) => {
          queue = queue.tail
          head.getVertices(Direction.OUT, "relation").filter(v => !queue.contains(v) && !(closedSet contains (Transformers.vertex2Url(v).location))
          ).foreach(v => {
            val url: Url = v
            if (url.status == UrlStatus.NEW && !urls.contains(url)) {
              urls += url
            } else if (url.status == UrlStatus.COMPLETE && !urls.contains(url)) {
              queue += v
            }
          })
          //ТОЛЬКО РОДИТЕЛЬСКИЙ УЗЕЛ!!!!
          closedSet += Transformers.vertex2Url(head).location
        }
      }
    }
    require(urls.toList.size == urls.toList.distinct.size)
    urls.toList
  }

}