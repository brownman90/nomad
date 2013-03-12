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
package com.nevilon.nomad.filter

import com.nevilon.nomad.logs.Logs
import com.nevilon.nomad.crawler.{URLUtils, Relation}

class UrlsCleaner extends Logs {

  def cleanUrls(linksToClear: List[Relation], startUrl: String): List[Relation] = {
    // linksToClear.
    //    map(_ => removeIncorrectUrls(_)).
    //      map(_ => normalizeUrls(_)).
    //      map(_ => removeSelfUrls(_)).
    //      map(_ => removeUrlsToAnotherDomain(_, startUrl)).
    //      map(_ => removeDuplicateUrls(_))

    val withoutIncorrect = removeIncorrectUrls(linksToClear)
    val normalized = normalizeUrls(withoutIncorrect)
    val withoutSelfUrls = removeSelfUrls(normalized)
    //val withoutUrlsToAnotherDomain = removeUrlsToAnotherDomain(withoutSelfUrls, startUrl)
    val withoutDuplicates = removeDuplicateUrls(withoutSelfUrls)
    withoutDuplicates
  }

  private def removeIncorrectUrls(linksToClear: List[Relation]): List[Relation] = {
    linksToClear.
      filter(urlRelation => !urlRelation.to.location.trim().isEmpty).
      filter(urlRelation => !urlRelation.to.location.contains("@")).
      filter(urlRelation => !urlRelation.to.location.startsWith("mailto:"))
  }

  private def removeSelfUrls(linksToClear: List[Relation]): List[Relation] = {
    linksToClear.filter(relation => !relation.from.equals(relation.to))
  }

  private def normalizeUrls(linksToClear: List[Relation]): List[Relation] = {
    linksToClear.map(relation => {
      val normalizedLocation = URLUtils.normalize(relation.to.location)
      new Relation(relation.from, relation.to.updateLocation(normalizedLocation))
    })
  }

//  private def removeUrlsToAnotherDomain(linksToClear: List[Relation], startUrl: String): List[Relation] = {
//    linksToClear.filter(urlRelation => {
//      try {
//        //accept links from this domain only!
//        val startDomain = URLUtils.getDomainName(startUrl)
//        val linkDomain = URLUtils.getDomainName(urlRelation.to.location)
//        startDomain.equals(linkDomain)
//      }
//      catch {
//        case e: Exception => {
//          error("error during clearLinks", e)
//        }
//        false
//      }
//    })
//  }


  private def removeDuplicateUrls(linksToClear: List[Relation]) = linksToClear.distinct

}
