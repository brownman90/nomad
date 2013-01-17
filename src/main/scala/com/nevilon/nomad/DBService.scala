package com.nevilon.nomad

import com.orientechnologies.orient.core.index.OIndexException
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery
import com.orientechnologies.orient.core.id.ORID
import collection.mutable.ArrayBuffer
import com.nevilon.nomad.UrlStatus.UrlStatus
import javax.naming.LinkRef
import com.orientechnologies.orient.core.tx.OTransaction.TXTYPE
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert
import com.orientechnologies.orient.core.config.OGlobalConfiguration


/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/10/13
 * Time: 5:23 AM
 */
class DBService {

  val dbManager = new OrientDBManager(true)
  val database = dbManager.getDatabase()


  /*
  def addDomain(domainName: String) {
    getDomain(domainName) match {
      case None => connector.addDomain(domainName)
      case Some(domain) =>
    }
  }
  */

  /*
  def getDomain(domainName: String): Option[Domain] = {
    connector.getDomain(domainName) match {
      case None => None
      case Some(doc) => {
        val domain = Transoformers.document2Domain(doc)
        Some(domain)
      }
    }
  }
  */

  def getOrCreateUrl(pageUrl: String): ODocument = {
    getUrl(pageUrl) match {
      case None => {
        addUrl(pageUrl)
        //create url
      }
      case Some(doc) => doc
    }

    /*
    try {
      Transoformers.document2Url(addUrl(pageUrl))
    }
    catch {
      case e: OIndexException => {
        //implement some kind of cache
        println("copy")
        getUrl(pageUrl) match {
          case None => throw new RuntimeException("Holy shit!")
          case Some(doc) => Transoformers.document2Url(doc)
        }
      }
    }
    */
  }

  def getUrl(pageUrl: String): Option[ODocument] = {
    val query = new OSQLSynchQuery[ODocument]("select from url WHERE location = ? ")
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


  // def linkUrls(parentPageUrl: String, childPagedUrl: String) {
  def linkUrls(relations: List[Types.LinkRelation]) {
    var counter = 0
    relations.foreach(relation => {

      val start = System.currentTimeMillis()
      val parentPage = getOrCreateUrl(relation._1)
      val childPage = getOrCreateUrl(relation._2)

      database.createEdge(parentPage, childPage).save()

      println("linkUrls: " + (System.currentTimeMillis() - start) + " counter: " + counter + " total: " + relations.length)
      counter+=1


    })
  }


  def addUrl(pageUrl: String): ODocument = {
    val pageNode = database.createVertex("url").field("location", pageUrl).field("status", UrlStatus.New)
    pageNode.save
    pageNode
  }

  def updateUrlStatus(url: String, urlStatus: UrlStatus) {
    getUrl(url) match {
      case None => throw new RuntimeException("Sorry, url not found!")
      case Some(doc) => {
        doc.field("status", urlStatus)
        doc.save()
      }
    }
  }


  def getBFSLinks(url: String, limit: Int): List[Url] = {
    println("get bfs links")
    //traverse page from (select from page WHERE url=?) #10:1234 while $depth <= 3
    val urlDoc = getUrl(url)
    urlDoc match {
      case None => throw new RuntimeException("Holy shit!")
      case Some(doc) => {
        val p = Transoformers.document2Url(doc)

        val query = new OSQLSynchQuery[ODocument]("select from (traverse V.out, E.in from " + p.id + " where $depth <= 15) where @class = 'url' AND status='NEW' limit " + limit)
        val result = database.query[java.util.List[ODocument]](query, p.id)
        val bfsUrls = new ArrayBuffer[Url]()
        import scala.collection.JavaConversions._
        result.foreach(odoc => {
          bfsUrls += Transoformers.document2Url(odoc)
        })
        bfsUrls.toList

      }

    }

  }


}
