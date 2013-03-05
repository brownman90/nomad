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
import com.nevilon.nomad.storage.graph.{SynchronizedDBService, TitanDBService}
import com.nevilon.nomad.logs.Logs
import com.nevilon.nomad.boot.GlobalConfig

trait PopProvider {

  def pop(): Option[Url]

}

class LinkProvider(domain: String, dbService: SynchronizedDBService) extends PopProvider with Logs {

  private val extractedLinks = new ListBuffer[Relation]
  private val linksToCrawl = new mutable.ArrayStack[Url]

  private val BFS_LIMIT = GlobalConfig.linksConfig.bfsLimit
  private val EXTRACTED_LINKS_LIMIT = GlobalConfig.linksConfig.extractedLinksCache


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
    //do not update if already present!!!
    dbService.getUrl(url) match {
      case None => {
        dbService.saveOrUpdateUrl(new Url(url, UrlStatus.NEW))
        dbService.addUrlToDomain(url)
      }
      case Some(v) => info("url " + url + " is already saved")
    }

  }

  def addToExtractedLinks(linkRelation: Relation) {
    // synchronized {
    extractedLinks += linkRelation
    //   }
  }

  private def urlToCrawl(): Option[Url] = {
    if (extractedLinks.length >= EXTRACTED_LINKS_LIMIT || linksToCrawl.size == 0) {
      flushExtractedLinks()
    }
    if (linksToCrawl.size == 0) {
      val links = loadLinksForCrawling(domain)
      if (links.size == 0) {
        None
      }
      else {
        linksToCrawl ++= links
        Some(linksToCrawl.pop())
      }
    } else {
      Some(linksToCrawl.pop())
    }
  }


  def flushExtractedLinks() {
    //  synchronized {
    dbService.linkUrls(extractedLinks.toList)
    info("flushed: " + extractedLinks.length + " link(s)")
    extractedLinks.clear()
    //  }
  }

  private def loadLinksForCrawling(startUrl: String): List[Url] = {
    //val domain_ = URLUtils.getDomainName(s)
    //TODO extract domain here!!!
    val bfsLinks = dbService.getLinksToCrawl(startUrl, BFS_LIMIT)
    info("bfs links loaded: " + bfsLinks.size)
    bfsLinks
  }

  def pop(): Option[Url] = {
    synchronized {
      urlToCrawl()
    }

  }
}
