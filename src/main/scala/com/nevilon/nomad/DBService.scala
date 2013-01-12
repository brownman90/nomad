package com.nevilon.nomad

import com.orientechnologies.orient.core.index.OIndexException
import com.orientechnologies.orient.core.record.impl.ODocument


/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/10/13
 * Time: 5:23 AM
 */
class DBService {

  val connector = new RawDBOrientConnector

  def addDomain(domainName: String) {
    getDomain(domainName) match {
      case None => connector.addDomain(domainName)
      case Some(domain) =>
    }
  }

  def getDomain(domainName: String): Option[Domain] = {
    connector.getDomain(domainName) match {
      case None => None
      case Some(doc) => {
        val domain = Transoformers.document2Domain(doc)
        Some(domain)
      }
    }
  }

  def getOrCreatePage(pageUrl: String): Page = {
    try {
      Transoformers.document2Page(connector.addPage(pageUrl))
    }
    catch {
      case e:OIndexException => {
        println("copy")
        connector.getPage(pageUrl) match {
          case None => throw new RuntimeException("Holy shit!")
          case Some(doc) => Transoformers.document2Page(doc)
        }
      }
    }
  }

  def addPage(parentPageUrl: String, childPagedUrl: String) {
    val parentPage = getOrCreatePage(parentPageUrl)
    val childPage = getOrCreatePage(childPagedUrl)
    connector.linkPages(parentPage.id, childPage.id)
  }

}
