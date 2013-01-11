package com.nevilon.nomad


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
      case Some(domain)=>
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
    val page = connector.getPage(pageUrl)
    page match {
      case None => {
        val doc = connector.addPage(pageUrl)
        Transoformers.document2Page(doc)
      }
      case Some(doc) => {
        Transoformers.document2Page(doc)
      }
    }
  }

  def addPage(parentPageUrl: String, childPagedUrl: String) {
    val parentPage = getOrCreatePage(parentPageUrl)
    val childPage = getOrCreatePage(childPagedUrl)
    connector.linkPages(parentPage.id, childPage.id)
  }

}
