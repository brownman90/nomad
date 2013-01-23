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
class LinkExtractor {


  def extractLinks(htmlString:String, host:String): ListBuffer[String] = {
    var links = ListBuffer[String]()
    val doc = Jsoup.parse(htmlString, host)
    import scala.collection.JavaConversions._
    for (link <- doc.select("a")) {
      val absHref: String = link.attr("abs:href")
      links += absHref
    }
    links
  }


}
