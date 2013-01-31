package com.nevilon.nomad.crawler

import java.net.URI
import org.apache.log4j.LogManager
import com.nevilon.nomad.filter.Action
import org.apache.commons.httpclient.util.URIUtil

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
    URIUtil.encodeQuery(url)
  }

  def getDomainName(url: String): String = {
    //NPE on urls like http://правительство.рф/gov/results/22601
    val uri = new URI(url.toLowerCase)
    val domain = uri.getHost
    //println(url)
    if (domain.startsWith("www.")) {
      domain.substring(4)
    } else {
      domain
    }
  }


  def clearUrlRelations(startUrl: String, linksToClear: List[RawUrlRelation]): List[RawUrlRelation] = {
    var clearedUrlRelations = List[RawUrlRelation]()
    clearedUrlRelations = linksToClear.
      filter(urlRelation => !urlRelation.to.contains("@")).
      filter(urlRelation => !urlRelation.to.startsWith("mailto:")).
      filter(urlRelation => !urlRelation.to.trim().isEmpty)
    //remove empty links
    //normalization
    //normalize from?
    clearedUrlRelations = clearedUrlRelations.map(urlRelation => new RawUrlRelation(urlRelation.from, URLUtils.normalize(urlRelation.to), Action.None))
    clearedUrlRelations = clearedUrlRelations.filter(urlRelation => !urlRelation.from.equals(urlRelation.to)) // check this!!!!)
    //remove links to another domains
    clearedUrlRelations = clearedUrlRelations.filter(urlRelation => {
      try {
        //accept links from this domain only!
        val startDomain = URLUtils.getDomainName(startUrl)
        val linkDomain = URLUtils.getDomainName(urlRelation.to)
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
    clearedUrlRelations.distinct
  }

}
