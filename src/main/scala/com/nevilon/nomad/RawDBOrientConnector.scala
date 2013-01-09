package com.nevilon.nomad

import com.orientechnologies.orient.core.db.graph.OGraphDatabase
import com.orientechnologies.orient.core.metadata.schema.OClass

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/9/13
 * Time: 3:53 PM
 * To change this template use File | Settings | File Templates.
 */
object RawDBOrientConnector {

  val database = new OGraphDatabase("remote:localhost/nomad")

  //root domain
  def addDomain() {}

  //page
  /*
    what if page's domain is new? We can create domain, but filter any but current domain during
    traversing?

   */
  def addVertex() {}

  //from to
  def addPagesEdge() {}

  //pages not proccessd yet
  def getFrontierVertexes() {}

  def findVertexByUrl() {}

  def connect() {
    database.open("admin", "admin")
  }

  def close() {
    database.close()
  }

  def demoUrls() {
    database.setUseCustomTypes(true)
    //val pagesClass = database.createVertexType("pages")

    val rootNode = database.createVertex("pages").field("root", "internet")
    //add lenta node
    val lentaNode = database.createVertex("pages").field("url", "lenta.ru")
    database.createEdge(rootNode, lentaNode)

    var currentNode = rootNode

    for (i <- 1 to 5000) {
      val lentaPageNode = database.createVertex("pages").field("url", "lenta.ru/" + i.toString)
      database.createEdge(lentaPageNode, lentaNode)
    }

    database.setRoot("internet", rootNode)
  }

  def main(args: Array[String]) {
    connect()
    demoUrls()
    close()
  }

}
