package com.nevilon.nomad.crawler

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
    //get status value
    val statusProperty = vertex.getProperty("status")
    val status = UrlStatus.withName(statusProperty.toString)
    //get action value
    //get str value of property by calling toString
    implicit def AnyRef2String(property: AnyRef) = property.toString
    new Url(vertex.getProperty("location"), status, vertex.getId,
      vertex.getProperty("fileId"))
  }

}
