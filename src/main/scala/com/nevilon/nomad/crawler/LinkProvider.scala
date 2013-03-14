/**
 * Copyright (C) 2012-2013 Vadim Bartko (vadim.bartko@nevilon.com).
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * See file LICENSE.txt for License information.
 */
package com.nevilon.nomad.crawler

import collection.mutable.ListBuffer
import collection.mutable
import com.nevilon.nomad.storage.graph.SynchronizedDBService
import com.nevilon.nomad.logs.Logs
import com.nevilon.nomad.boot.GlobalConfig

trait PopProvider {

  def pop(): Option[Url]

}

class LinkProvider(domain: Domain, dbService: SynchronizedDBService) extends PopProvider with Logs {

  private val extractedLinks = new ListBuffer[Relation]
  private val linksToCrawlQueue = new mutable.ArrayStack[Url]

  private val LINKS_QUEUE_LIMIT = GlobalConfig.linksConfig.queueLimit
  private val EXTRACTED_LINKS_LIMIT = GlobalConfig.linksConfig.extractedLinksCache

  def findOrCreateUrl(url: String) {
    dbService.getUrl(url) match {
      case None => {
        val savedUrl = dbService.saveOrUpdateUrl(new Url(url, UrlStatus.NEW))
        dbService.addUrlToDomain(savedUrl)
      }
      case Some(v) => info("url " + url + " is already saved")
    }

  }

  def addToExtractedLinks(linkRelation: Relation) = extractedLinks += linkRelation

  private def urlToCrawl(): Option[Url] = {
    if (extractedLinks.length >= EXTRACTED_LINKS_LIMIT || linksToCrawlQueue.size == 0) flushExtractedLinks()
    if (linksToCrawlQueue.size == 0) {
      val links = loadLinksForCrawling(domain)
      if (links.size == 0) None
      else {
        linksToCrawlQueue ++= links
        Some(linksToCrawlQueue.pop())
      }
    } else Some(linksToCrawlQueue.pop())
  }


  def flushExtractedLinks() {
    dbService.linkUrls(extractedLinks.toList)
    info("flushed: " + extractedLinks.length + " link(s)")
    extractedLinks.clear()
  }

  private def loadLinksForCrawling(domain: Domain): List[Url] = {
    val linksToCrawl = dbService.getLinksToCrawl(domain, LINKS_QUEUE_LIMIT)
    info("links to crawl  loaded: " + linksToCrawl.size)
    linksToCrawl
  }

  def pop(): Option[Url] =  urlToCrawl()

}
