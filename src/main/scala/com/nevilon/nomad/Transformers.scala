package com.nevilon.nomad

import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.id.ORID

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/10/13
 * Time: 5:23 AM
 * To change this template use File | Settings | File Templates.
 */
object Transoformers {

  def document2Page(doc: ODocument): Page = {
    val link = new Page(doc.field("url"), doc.getIdentity)
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

class Page(val url: String, val id: ORID = null) {

}

