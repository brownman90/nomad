package com.nevilon.nomad

import java.net.URI

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/11/13
 * Time: 12:29 PM
 */
object URLUtils {

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

}
