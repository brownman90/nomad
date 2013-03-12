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

import com.nevilon.nomad.storage.graph.SynchronizedDBService
import com.nevilon.nomad.filter.{UrlsCleaner, Action, FilterProcessorFactory}
import org.apache.http.HttpEntity
import java.io.{ByteArrayInputStream, InputStream}
import org.apache.http.util.EntityUtils
import com.nevilon.nomad.logs
import logs.{Logs, Statistics}
import com.nevilon.nomad.boot.GlobalConfig


class Worker(val domain: Domain, val startUrl: String, val maxThreads: Int,
             dbService: SynchronizedDBService, contentSaver: ContentSaver,
             onCrawlingComplete: (Worker) => Unit) extends Logs {


  private val cw = new CounterWrapper(domain)

  private val linkProvider = new LinkProvider(domain, dbService)
  linkProvider.findOrCreateUrl(URLUtils.normalize(startUrl))
  private val pageDataExtractor = new PageDataExtractor

  private val filterProcessor = FilterProcessorFactory.get(URLUtils.normalize(startUrl))


  private val httpClient = HttpClientFactory.buildHttpClient(maxThreads, maxThreads, GlobalConfig.appConfig.userAgent)

  private val carousel = new Carousel(maxThreads, linkProvider)
  carousel.setOnStart((url: Url) => loadAndProcess(url))
  carousel.setOnBeforeStart((url: Url) => {
    (dbService.saveOrUpdateUrl(url.updateStatus(UrlStatus.IN_PROGRESS)))
  })

  carousel.setOnCrawlingComplete(() => {
    httpClient.getConnectionManager.shutdown()
    linkProvider.flushExtractedLinks()
    info("crawling complete " + startUrl)
    onCrawlingComplete(this)
    //flush links
  })


  def stop(softly: Boolean) {
    //stop carousel
    info("sending stop command to carousel " + startUrl)
    carousel.stop(softly)
    //flush links
  }


  private def loadAndProcess(url2: Url) {
    cw.crawledCounter.inc()
    val url = url2.updateStatus(UrlStatus.IN_PROGRESS)
    dbService.saveOrUpdateUrl(url)
    //drop link here?
    dbService.removeUrlFromDomain(url2)

    val fetcher = new Fetcher(url, httpClient)
    fetcher.onException((e: Exception) => {
      dbService.saveOrUpdateUrl(url.updateStatus(UrlStatus.ERROR))
      info("error during crawling " + url.location, e)
      cw.errorCounter.inc()
    })
    fetcher.onHttpError((code: Int) => {
      dbService.saveOrUpdateUrl(url.updateStatus(UrlStatus.HTTP_ERROR))
      info("http error during crawling " + url.location + " error " + code)
      cw.httpErrorCounter.inc()
    })

    fetcher.onFinish(() => {
      //      carousel.start()
    })

    fetcher.onDataStream((entityParams: EntityParams, entity: HttpEntity, url: Url) => {
      if (filterProcessor.filterEntity(entityParams) == Action.Download) {

        val data: (InputStream, Option[String]) = {
          if (entityParams.mimeType.getSubType.contains("html")) {
            val contentAsTxt = EntityUtils.toString(entity)
            (new ByteArrayInputStream(contentAsTxt.getBytes), Some(contentAsTxt))
          } else {
            (entity.getContent, None)
          }
        }
        val gfsId = contentSaver.saveContent(data._1, url.location, entityParams.mimeType.getBaseType, url.id)

        val fetchedContent = {
          data._2 match {
            case None => {
              //binary file
              new ExtractedData(Nil, new FetchedContent(gfsId, entityParams, null))
            }
            case Some(content) => {
              //html file
              processHtmlContent(new FetchedContent(gfsId, entityParams, content), url)
            }
          }
        }
        onProcessingComplete(fetchedContent, url)
      } else {
        dbService.saveOrUpdateUrl(url.updateStatus(UrlStatus.SKIP))
        info("skipped entity " + url.location)
        cw.skippedFileCounter.inc()
      }
    })
    fetcher.load()
  }


  private def processHtmlContent(value: FetchedContent, url: Url): ExtractedData = {
    val page = pageDataExtractor.extractLinks(value.content, url.location)
    info("links extracted: " + page.links.length + " from " + url.location)
    //build urlrelations objects
    val relations = page.links.map(item => {
      new Relation(url, new Url(item.url, UrlStatus.NEW))
    })
    //remove invalid links


    val clearedLinks = new UrlsCleaner().cleanUrls(relations.toList, startUrl)

    val (internalLinks, externalLinks) = clearedLinks.partition(relation => {
      val startDomain = URLUtils.getDomainName(startUrl)
      val linkDomain = URLUtils.getDomainName(relation.to.location)
      //println(startDomain + " " + linkDomain + " " + startDomain.equals(linkDomain))
      startDomain.equals(linkDomain)
    })


    val acceptedExternalLinks =  externalLinks.map(relation=>{
      val linkDomain = URLUtils.getDomainName(URLUtils.normalize(relation.to.location))
      val action = filterProcessor.filterDomain(linkDomain)
      val toUrl = relation.to.updateStatus(action2UrlStatus(action))
      new Relation(relation.from, toUrl)
    }).filter(relation=>relation.to.status==UrlStatus.NEW)

    //pass to filter
    val filteredRawUrlRelations = internalLinks.map(relation => {
      val action = filterProcessor.filterUrl(relation.to.location)

      val status = {
        action match {
          case Action.Download => UrlStatus.NEW
          case Action.Skip => {
            cw.skippedUrlCounter.inc()
            info("skipped url " + relation.to.location)
            UrlStatus.SKIP
          }
        }
      }

      val toUrl = relation.to.updateStatus(status)
      new Relation(relation.from, toUrl)
    })

    new ExtractedData(filteredRawUrlRelations:::acceptedExternalLinks, value)
  }

  private def action2UrlStatus(action: Action.Value) = action match {
    case Action.Download => UrlStatus.NEW
    case Action.Skip => UrlStatus.SKIP
    case _=>throw  new RuntimeException("None Action!")
  }

  private def onProcessingComplete(extractedData: ExtractedData, url: Url) {
    println("onProcessingCompletele")
    dbService.saveOrUpdateUrl(
      url.
        updateStatus(UrlStatus.COMPLETE).
        updateFileId(extractedData.fetchedContent.gfsId)
    )
    extractedData.relations.foreach(linkProvider.addToExtractedLinks(_))
  }


}

class CounterWrapper(domain: Domain) {

  private val counterGroup = Statistics.createCounterGroup(domain.name)

  val skippedUrlCounter = counterGroup.createCounter("skipped urls")
  val skippedFileCounter = counterGroup.createCounter("skipped files")
  val crawledCounter = counterGroup.createCounter("crawled urls")
  val errorCounter = counterGroup.createCounter("errors")
  val httpErrorCounter = counterGroup.createCounter("http errors")


}