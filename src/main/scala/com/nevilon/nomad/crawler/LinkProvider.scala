package com.nevilon.nomad.crawler

import collection.mutable.ListBuffer
import collection.mutable
import org.apache.log4j.LogManager
import com.nevilon.nomad.storage.graph.TitanDBService

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/20/13
 * Time: 4:11 PM
 */
class LinkProvider(domain: String, dbService: TitanDBService) {

  private val extractedLinks = new ListBuffer[RawUrlRelation]
  private val linksToCrawl = new mutable.ArrayStack[Url]

  private val BFS_LIMIT = 1000


  private val logger = LogManager.getLogger(this.getClass.getName)

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
      if (extractedLinks.length >= 30000) {
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
      logger.info("flushed: " + extractedLinks.length + " link(s)")
      extractedLinks.clear()
    }
  }

  private def loadLinksForCrawling(startUrl: String): List[Url] = {
    val bfsLinks = dbService.getBFSLinks(startUrl, 1000)
    logger.info("bfs links loaded: "+BFS_LIMIT)
    bfsLinks.toList
  }

}
