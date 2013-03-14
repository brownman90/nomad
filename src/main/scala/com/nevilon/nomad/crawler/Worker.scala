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
import com.nevilon.nomad.filter.{Action, FilterProcessorFactory}
import org.apache.http.HttpEntity
import com.nevilon.nomad.logs
import logs.{Logs, Statistics}


class Worker(val domain: Domain, val startUrl: String, val maxThreads: Int,
             dbService: SynchronizedDBService, contentSaver: ContentSaver,
             onCrawlingComplete: (Worker) => Unit) extends Logs {

  private implicit val cw = new CounterWrapper(domain)
  private val linkProvider = new LinkProvider(domain, dbService)
  private val filterProcessor = FilterProcessorFactory.get(URLUtils.normalize(startUrl))
  private val httpClient = HttpClientFactory.
    buildHttpClient(maxThreads, maxThreads, UserAgentProvider.getUserAgentString())

  private val contentProcessor = new ContentProcessor(contentSaver, filterProcessor, startUrl)

  //create Carousel impl
  private val carousel = new Carousel(maxThreads, linkProvider) {

    def onBeforeStart(url: Url) = dbService.saveOrUpdateUrl(url.updateStatus(UrlStatus.IN_PROGRESS))

    def onCrawlingComplete() {
      httpClient.getConnectionManager.shutdown()
      linkProvider.flushExtractedLinks()
      info("crawling complete " + startUrl)
      Worker.this.onCrawlingComplete(Worker.this)
    }

    def onStartMethod(url: Url) = loadAndProcess(url)

  }

  //extend Fetcher class
  class WorkerFetcher(url: Url) extends Fetcher(url, httpClient) {
    def onException(e: Exception) {
      dbService.saveOrUpdateUrl(url.updateStatus(UrlStatus.ERROR))
      info("error during crawling " + url.location, e)
      cw.errorCounter.inc()
    }

    def onHttpError(code: Int): Unit = {
      dbService.saveOrUpdateUrl(url.updateStatus(UrlStatus.HTTP_ERROR))
      info("http error during crawling " + url.location + " error " + code)
      cw.httpErrorCounter.inc()
    }

    def onDataStream(entityParams: EntityParams, entity: HttpEntity, url: Url): Unit = {
      if (filterProcessor.filterEntity(entityParams) == Action.Download) {
        val (gfsId, relations) = contentProcessor.processDataStream(entityParams, entity, url)
        println("onProcessingCompletele")
        dbService.saveOrUpdateUrl(
          url.
            updateStatus(UrlStatus.COMPLETE).
            updateFileId(gfsId)
        )
        relations.foreach(linkProvider.addToExtractedLinks(_))
      }
      else {
        dbService.saveOrUpdateUrl(url.updateStatus(UrlStatus.SKIP))
        info("skipped entity " + url.location)
        cw.skippedFileCounter.inc()
      }
    }
  }

  carousel.start()


  private def loadAndProcess(url: Url) {
    cw.crawledCounter.inc()
    dbService.saveOrUpdateUrl(url.updateStatus(UrlStatus.IN_PROGRESS))
    //drop link here?
    dbService.removeUrlFromDomain(url)
    val fetcher = new WorkerFetcher(url)
    fetcher.load()
  }


  def stop(softly: Boolean) {
    info("sending stop command to carousel " + startUrl)
    carousel.stop(softly)
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