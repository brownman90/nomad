package com.nevilon.nomad.crawler

import java.net.URI
import org.apache.log4j.LogManager

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/11/13
 * Time: 12:29 PM
 */
object URLUtils {

  private val logger = LogManager.getLogger(this.getClass.getName)

  def normalize(urlValue: String): String = {
    var url = urlValue.trim
    if (url.contains("www.")) {
      url = url.replace("www.", "")
    }
    if (!(url.startsWith("http://") || url.startsWith("https://"))) {
      url = "http://" + url
    }
    if (url.endsWith("/")) {
      url = url.substring(0, url.length - 1)
    }
    if (url.contains("#")) {
      url = url.substring(0, url.indexOf("#"))
    }
    url
  }

  def getDomainName(url: String): String = {
    val uri = new URI(url.toLowerCase)
    val domain = uri.getHost
    //println(url)
    if (domain.startsWith("www.")) {
      domain.substring(4)
    } else {
      domain
    }
  }


  def clearLinks(startUrl:String,linksToClear: List[RawUrlRelation]): List[RawUrlRelation] = {
    var clearedLinks = List[RawUrlRelation]()
    //remove email links
    clearedLinks = linksToClear.filter(url => !url.to.contains("@"))
    clearedLinks = clearedLinks.filter(url => !url.to.startsWith("mailto:"))
    //remove empty links
    clearedLinks = clearedLinks.filter(newLink => !newLink.to.trim().isEmpty)
    //normalization
    //normalize from?
    clearedLinks = clearedLinks.map(newLink => new RawUrlRelation(newLink.from, URLUtils.normalize(newLink.to)))
    clearedLinks = clearedLinks.filter(newLink => !newLink.from.equals(newLink.to)) // check this!!!!)
    //remove links to another domains
    clearedLinks = clearedLinks.filter(newLink => {
      try {
        //accept links from this domain only!
        val startDomain = URLUtils.getDomainName(startUrl)
        val linkDomain = URLUtils.getDomainName(newLink.to)
        startDomain.equals(linkDomain)
      }
      catch {
        case e: Exception => {
          logger.error("error during clearLinks", e)
        }
        false
      }
    })
    //remove duplicates
    clearedLinks.distinct
  }

}
