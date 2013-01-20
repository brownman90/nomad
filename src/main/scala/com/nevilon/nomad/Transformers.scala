package com.nevilon.nomad

import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.id.ORID
import com.tinkerpop.blueprints.Vertex
import org.apache.commons.lang.builder.{EqualsBuilder, HashCodeBuilder}

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/10/13
 * Time: 5:23 AM
 * To change this template use File | Settings | File Templates.
 */


object Transformers {

  def vertex2Url(vertex: Vertex): Url = {
    println(vertex)
    val status = UrlStatus.withName(vertex.getProperty("status").toString)
    new Url(vertex.getProperty("location").toString, status, vertex.getId.toString)
  }


}
