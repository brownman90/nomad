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

import org.jsoup.Jsoup
import collection.mutable.ListBuffer
import scala.Predef._

class PageDataExtractor {


  def extractLinks(htmlString: String, host: String): Page = {
    val doc = Jsoup.parse(htmlString, host)

    import scala.collection.JavaConversions._
    //extract links
    var links = ListBuffer[PageLink]()
    for (link <- doc.select("a")) {
      links += new PageLink(link.attr("abs:href"), link.text())

    }
    //extract title
    val title: Option[String] = {
      val titleElements = doc.select("title")
      if (!titleElements.isEmpty) {
        Some(titleElements.head.text())
      } else {
        None
      }
    }
    new Page(title, links.toList)
  }


}

class PageLink(val url: String, val name: String)

class Page(val title: Option[String], val links: List[PageLink])