package com.nevilon.nomad

import com.tinkerpop.blueprints.Vertex

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/10/13
 * Time: 5:23 AM
 * To change this template use File | Settings | File Templates.
 */


object Transformers {


  def vertex2Url(vertex: Vertex): Url = {
    val statusProperty = vertex.getProperty("status")
    val status = UrlStatus.withName(statusProperty.toString)
    new Url(vertex.getProperty("location").toString, status, vertex.getId.toString)
  }

}
