package com.nevilon.nomad

import com.orientechnologies.orient.core.db.graph.OGraphDatabase
import com.orientechnologies.orient.core.metadata.schema.OClass
import com.orientechnologies.orient.core.record.impl.{ODocument, ORecordBytes}
import java.io.{File, FileInputStream, BufferedInputStream}
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import com.orientechnologies.orient.core.id.ORID

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/9/13
 * Time: 3:53 PM
 * To change this template use File | Settings | File Templates.
 */


class RawDBOrientConnector {

  val database = new OGraphDatabase("remote:localhost/nomad")
  connect()

  //root domain
  //use only normalized form like http(s)://www.google.com
  def addDomain(domainName: String) {
    val domainNode = database.createVertex("domain").field("name", domainName)
    domainNode.save()
  }

  //convert to some pojo object?
  def getDomain(domainName: String): Option[ODocument] = {
    val query = new OSQLSynchQuery[ODocument]("select from domain WHERE name = ? ")
    val result = database.query[java.util.List[ODocument]](query, domainName)
    if (result.isEmpty) {
      None
    } else {
      if (result.size() > 1) {
        throw new RuntimeException("By some really strange reasons there are more than one domain with this name!")
      } else {
        Some(result.get(0))
      }
    }
  }

  def addPage(pageUrl: String): ODocument = {
    val pageNode = database.createVertex("page").field("url", pageUrl)
    pageNode.save
    return pageNode
  }

  def getPage(pageUrl: String): Option[ODocument] = {
    val query = new OSQLSynchQuery[ODocument]("select from page WHERE url = ? ")
    val result = database.query[java.util.List[ODocument]](query, pageUrl)
    if (result.isEmpty) {
      None
    } else {
      if (result.size() > 1) {
        throw new RuntimeException("By some really strange reasons there are more than one page with this url!")
      } else {
        Some(result.get(0))
      }
    }
  }

  //page
  /*
    what if page's domain is new? We can create domain, but filter any but current domain during
    traversing?

   */
  def addVertex() {}

  def linkPages(parentPageORID: ORID, childPageORID: ORID) {
    database.createEdge(parentPageORID, childPageORID).save()
  }

  def isVertexTypeExists(vertexType: String): Boolean = {
    database.getVertexType(vertexType) != null
  }


  def createSkeleton() {
    if (database.getRoot("root") == null) {
      val rootNode = database.createVertex().field("root", "internet")
      database.setRoot("root", rootNode)
    }
  }

  def createTypes() {
    if (!isVertexTypeExists("page")) {
      database.createVertexType("page")
    }
    if (!isVertexTypeExists("domain")) {
      database.createVertexType("domain")
    }
  }

  def connect() {
    database.open("admin", "admin")
    database.setUseCustomTypes(true)
    createTypes()
  }

  def close() {
    database.close()
  }

  def demoUrls() {


    /*
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
    */
  }

  def main(args: Array[String]) {
    connect()
    demoUrls()
    close()
  }

}
