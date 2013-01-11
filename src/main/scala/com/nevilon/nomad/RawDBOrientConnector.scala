package com.nevilon.nomad

import com.orientechnologies.orient.core.db.graph.OGraphDatabase
import com.orientechnologies.orient.core.metadata.schema.{OType, OClass}
import com.orientechnologies.orient.core.record.impl.{ODocument, ORecordBytes}
import java.io.{File, FileInputStream, BufferedInputStream}
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import com.orientechnologies.orient.core.id.ORID
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE
import com.orientechnologies.orient.client.remote.OServerAdmin

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/9/13
 * Time: 3:53 PM
 * To change this template use File | Settings | File Templates.
 */


class RawDBOrientConnector {

  val dbManager = new OrientDBManager(true)
  val database = dbManager.getDatabase()

  //root domain
  //use only normalized form like http(s)://www.google.com
  def addDomain(domainName: String) {
    val domainNode = database.createVertex("Domain").field("name", domainName)
    domainNode.save()
  }

  //convert to some pojo object?
  def getDomain(domainName: String): Option[ODocument] = {
    val query = new OSQLSynchQuery[ODocument]("select from Domain WHERE name = ? ")
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

  def linkPages(parentPageORID: ORID, childPageORID: ORID) {
    database.createEdge(parentPageORID, childPageORID).save()
  }


  def createSkeleton() {
    if (database.getRoot("root") == null) {
      val rootNode = database.createVertex().field("root", "internet")
      database.setRoot("root", rootNode)
    }
  }


}
