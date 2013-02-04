package com.nevilon.nomad.crawler

import com.tinkerpop.blueprints.Vertex
import com.nevilon.nomad.filter.Action

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
    val actionProperty = vertex.getProperty("action")
    val action = Action.withName(actionProperty.toString)

    new Url(vertex.getProperty("location").toString, status, vertex.getId.toString,
      vertex.getProperty("fileId").toString, vertex.getProperty("title").toString, action)
  }

}
