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

import java.net.URI
import org.apache.commons.httpclient.util.URIUtil
import com.nevilon.nomad.logs.Logs

object URLUtils extends Logs {

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
    if (domain.startsWith("www.")) {
      domain.substring(4)
    } else {
      domain
    }
  }

  /*
  def getRootUri(url: String): String = {
    val normalized = normalize(url)
    //schema+host?
  }
  */

  //move to filter!!!!
//
}


