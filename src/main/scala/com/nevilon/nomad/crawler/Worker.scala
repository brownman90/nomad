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

import org.apache.http.client.HttpClient
import com.nevilon.nomad.storage.graph.{APIFacade, FileStorage, TitanDBService}
import com.nevilon.nomad.filter.{Action, FilterProcessorFactory}
import org.apache.http.HttpEntity
import java.io.{File, FileOutputStream, ByteArrayInputStream, InputStream}
import org.apache.http.util.EntityUtils
import com.nevilon.nomad.logs
import logs.{Logs, Statistics}
import org.apache.log4j.lf5.util.StreamUtils
import io.Source
import java.nio.file.{FileSystems, Path, Files}


class Worker(val startUrl: String, val maxThreads: Int,
             dbService: APIFacade,
             onCrawlingComplete: (Worker) => Unit) extends Logs {

  private val domain = URLUtils.getDomainName(URLUtils.normalize(startUrl))
  dbService.domainService.createDomainIfNeeded(domain)


  private val contentSaver = new ContentSaver(dbService)
  private val linkProvider = new LinkProvider(startUrl, dbService)
  linkProvider.findOrCreateUrl(URLUtils.normalize(startUrl))
  private val pageDataExtractor = new PageDataExtractor

  //dbService.test


  private val filterProcessor = FilterProcessorFactory.get(URLUtils.normalize(startUrl))


  private val counterGroup = Statistics.createCounterGroup(startUrl)

  private val skippedUrlCounter = counterGroup.createCounter("skipped urls")
  private val skippedFileCounter = counterGroup.createCounter("skipped files")
  private val crawledCounter = counterGroup.createCounter("crawled urls")
  private val errorCounter = counterGroup.createCounter("errors")
  private val httpErrorCounter = counterGroup.createCounter("http errors")

  private val httpClient = HttpClientFactory.buildHttpClient(maxThreads, maxThreads)

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
    crawledCounter.inc()
    val url = url2.updateStatus(UrlStatus.IN_PROGRESS)
    dbService.saveOrUpdateUrl(url)
    //drop link here?
    dbService.removeUrlFromDomain(URLUtils.getDomainName(URLUtils.normalize(startUrl)), url2.location)

    val fetcher = new Fetcher(url, httpClient)
    fetcher.onException((e: Exception) => {
      dbService.saveOrUpdateUrl(url.updateStatus(UrlStatus.ERROR))
      info("error during crawling " + url.location, e)
      errorCounter.inc()
    })
    fetcher.onHttpError((code: Int) => {
      dbService.saveOrUpdateUrl(url.updateStatus(UrlStatus.HTTP_ERROR))
      info("http error during crawling " + url.location + " error " + code)
      httpErrorCounter.inc()
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
        skippedFileCounter.inc()
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
    val clearedLinks = URLUtils.clearUrlRelations(startUrl, relations.toList)
    //pass to filter
    val filteredRawUrlRelations = clearedLinks.map(relation => {
      val action = filterProcessor.filterUrl(relation.to.location)

      val status = {
        action match {
          case Action.Download => UrlStatus.NEW
          case Action.Skip => {
            skippedUrlCounter.inc()
            info("skipped url " + relation.to.location)
            UrlStatus.SKIP
          }
        }
      }

      val toUrl = relation.to.updateStatus(status)
      new Relation(relation.from, toUrl)
    })

    new ExtractedData(filteredRawUrlRelations, value)
  }

  private def onProcessingComplete(extractedData: ExtractedData, url: Url) {
    dbService.saveOrUpdateUrl(
      url.
        updateStatus(UrlStatus.COMPLETE).
        updateFileId(extractedData.fetchedContent.gfsId)
    )
    extractedData.relations.foreach(linkProvider.addToExtractedLinks(_))
  }


}