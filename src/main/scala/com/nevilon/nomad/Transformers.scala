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
object Transoformers {

  def document2Url(doc: ODocument): Url = {
    val link = new Url(doc.field("location"), doc.getIdentity)
    return link
  }

  def document2Domain(doc: ODocument): Domain = {
    val domain = new Domain(doc.field("name"), doc.getIdentity)
    return domain
  }

  def domain2Document() {}

  def link2Document() {}
}


class Domain(val name: String, val id: ORID = null) {

}

class Url(val location: String, val id: ORID = null) {

}


object Transformers2 {

  def vertex2Url(vertex: Vertex): Url2 = {
    println(vertex)
    val status = UrlStatus.withName(vertex.getProperty("status").toString)
    new Url2(vertex.getProperty("location").toString, status , vertex.getId.toString)
  }


}


class Url2(val location: String, val status: UrlStatus.Value, val id: String) {
  override def equals(obj: Any): Boolean = {
    if (obj.isInstanceOf[Url2]) {
      val other = obj.asInstanceOf[Url2]
      new EqualsBuilder()
        .append(location, other.location)
        .isEquals()
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    new HashCodeBuilder()
      .append(location)
      .toHashCode()
  }
}
