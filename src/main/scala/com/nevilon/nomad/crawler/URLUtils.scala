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
import org.apache.log4j.LogManager
import org.apache.commons.httpclient.util.URIUtil

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
    if (domain.startsWith("www.")) {
      domain.substring(4)
    } else {
      domain
    }
  }


  def clearUrlRelations(startUrl: String, linksToClear: List[Relation]): List[Relation] = {
    var clearedUrlRelations = List[Relation]()
    clearedUrlRelations = linksToClear.
      filter(urlRelation => !urlRelation.to.location.contains("@")).
      filter(urlRelation => !urlRelation.to.location.startsWith("mailto:")).
      filter(urlRelation => !urlRelation.to.location.trim().isEmpty)
    //remove empty links
    //normalization
    //normalize from?
    clearedUrlRelations = clearedUrlRelations.map(relation => {
      val normalizedLocation = URLUtils.normalize(relation.to.location)
      new Relation(relation.from, relation.to.updateLocation(normalizedLocation))
    })

    clearedUrlRelations = clearedUrlRelations.filter(relation => !relation.from.equals(relation.to)) // check this!!!!)
    //remove links to another domains
    clearedUrlRelations = clearedUrlRelations.filter(urlRelation => {
      try {
        //accept links from this domain only!
        val startDomain = URLUtils.getDomainName(startUrl)
        val linkDomain = URLUtils.getDomainName(urlRelation.to.location)
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
