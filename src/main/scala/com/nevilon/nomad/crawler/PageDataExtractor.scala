package com.nevilon.nomad.crawler

import org.jsoup.Jsoup
import collection.mutable.ListBuffer
import scala.Predef._

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/11/13
 * Time: 2:18 PM
 */
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