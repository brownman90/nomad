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
import org.apache.http.HttpEntity
import com.nevilon.nomad.logs.Logs
import com.nevilon.nomad.filter.{UrlsCleaner, Action, FilterProcessor}
import org.apache.http.util.EntityUtils
import java.io.ByteArrayInputStream


class ContentProcessor(saver: ContentSaver, filterProcessor: FilterProcessor, startUrl: String)
                      (implicit cw: CounterWrapper) extends Logs {


  private val pageDataExtractor = new PageDataExtractor

  def processDataStream(entityParams: EntityParams, entity: HttpEntity, url: Url): (String, List[Relation]) = {
    val relations = new ListBuffer[Relation]
    val is =
      if (entityParams.mimeType.getSubType.contains("html")) {
        val contentAsTxt = EntityUtils.toString(entity)
        relations ++= extractLinks(contentAsTxt, url, startUrl)
        new ByteArrayInputStream(contentAsTxt.getBytes)
      } else entity.getContent
    val gfsId = saver.saveContent(is, url.location, entityParams.mimeType.getBaseType, url.id)
    (gfsId, relations.toList)
  }

  private implicit def action2UrlStatus(action: Action.Value) = action match {
    case Action.Download => UrlStatus.NEW
    case Action.Skip => UrlStatus.SKIP
    case _ => throw new RuntimeException("None Action!")
  }


  private def extractLinks(content: String, url: Url, startUrl: String): List[Relation] = {
    val page = pageDataExtractor.extractLinks(content, url.location)
    info("links extracted: " + page.links.length + " from " + url.location)

    val relations = page.links.map(item => new Relation(url, new Url(item.url, UrlStatus.NEW)))
    val clearedLinks = new UrlsCleaner().cleanUrls(relations.toList, startUrl)

    val startDomain = URLUtils.getDomainName(startUrl)
    val (internalLinks, externalLinks) = clearedLinks.partition(relation =>
      startDomain.equals(URLUtils.getDomainName(relation.to.location)))

    val acceptedExternalLinks = externalLinks.map(relation => {
      val linkDomain = URLUtils.getDomainName(URLUtils.normalize(relation.to.location))
      val action = filterProcessor.filterDomain(linkDomain)
      val toUrl = relation.to.updateStatus(action)
      new Relation(relation.from, toUrl)
    }).filter(relation => relation.to.status == UrlStatus.NEW)

    //pass to filter
    val filteredRawUrlRelations = internalLinks.map(relation => {
      val action = filterProcessor.filterUrl(relation.to.location)
      val toUrl = relation.to.updateStatus(action)
      new Relation(relation.from, toUrl)
    })

    filteredRawUrlRelations.foreach(relation =>
      if (relation.to.status == UrlStatus.SKIP) {
        cw.skippedUrlCounter.inc()
        info("skipped url " + relation.to.location)
      })

    filteredRawUrlRelations ::: acceptedExternalLinks
  }

}
