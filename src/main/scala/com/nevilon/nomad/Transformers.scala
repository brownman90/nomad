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

  /*this is workaround. By some reasons sometimes vertex is returned without properties
  Solution: if there is no properties:
     return None
     set status to New

   on next traverse:
       if status==New But has child - set status to Complete
          skip vertex
  */
  def vertex2Url(vertex: Vertex): Option[Url] = {
    val statusProperty = vertex.getProperty("status")
    if (statusProperty == null) {
      //log.error
      None
    } else {
      val status = UrlStatus.withName(statusProperty.toString)
      val url = new Url(vertex.getProperty("location").toString, status, vertex.getId.toString)
      Some(url)
    }

  }


}
