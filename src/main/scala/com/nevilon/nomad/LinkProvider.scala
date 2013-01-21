package com.nevilon.nomad

import storage.graph.TitanDBService
import collection.mutable.ListBuffer
import collection.mutable

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/20/13
 * Time: 4:11 PM
 */
class LinkProvider(domain: String, dbService: TitanDBService) {

  private val extractedLinks = new ListBuffer[RawUrlRelation]
  private val linksToCrawl = new mutable.ArrayStack[Url]


  /*
    url - normalized form
   */
  def findOrCreateUrl(url: String) {
    /*
       go to service
       do we need to check domains table?
       obviously we need domains table to choose domains for crawling
       maybe we should check(lookup for) link and than use domains table as while list?
       so here we need just find url in urls table and than check if domain is in white list(domains table)

     */
    dbService.getOrCreateUrl(url)
  }

  def addToExtractedLinks(linkRelation: RawUrlRelation) {
    extractedLinks += linkRelation
  }

  def urlToCrawl(): Option[Url] = {
    if (linksToCrawl.size == 0) {
      flushExtractedLinks()
      val links = loadLinksForCrawling(domain)
      if (links.size == 0) None
      else {
        linksToCrawl ++= links
        Some(linksToCrawl.pop())
      }
    } else {
      if (extractedLinks.length >= 10000) {
        flushExtractedLinks()
      }
      Some(linksToCrawl.pop())
    }
  }


  def updateUrlStatus(url: String, urlStatus: UrlStatus.Value) {
    dbService.updateUrlStatus(url, urlStatus)
  }

  def flushExtractedLinks() {
    this.synchronized {
      dbService.linkUrls(extractedLinks.toList)
      println("flushed: " + extractedLinks.length)
      extractedLinks.clear()
    }
  }

  private def loadLinksForCrawling(startUrl: String): List[Url] = {
    val bfsLinks = dbService.getBFSLinks(startUrl, 5000)
    println("loadLinksForCrawling " + bfsLinks.size)
    bfsLinks.toList
  }

}
