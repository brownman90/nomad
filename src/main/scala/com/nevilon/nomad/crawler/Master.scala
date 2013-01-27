package com.nevilon.nomad.crawler

import collection.mutable.ListBuffer
import concurrent._
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.{HttpEntity, HttpResponse}
import org.apache.http.util.EntityUtils
import scala.util.Success
import org.apache.log4j.LogManager
import com.nevilon.nomad.storage.graph.TitanDBService
import com.nevilon.nomad.filter.{Action, FilterProcessor, FilterProcessorFactory}

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/14/13
 * Time: 7:51 AM
 */

class Master {

  private val logger = LogManager.getLogger(this.getClass.getName)

  private val MAX_THREADS = 10
  private val NUM_OF_DOMAINS = 1

  private val httpClient = HttpClientFactory.buildHttpClient(MAX_THREADS * NUM_OF_DOMAINS, MAX_THREADS)
  private val dbService = new TitanDBService(true) //DBService


  def startCrawling() {
    //run each in separate thread?
    // or run thread inside crawler?
    logger.info("starting workerks")
    //
    val worker = new Worker("http://aaai.org", MAX_THREADS, httpClient, dbService)
    worker.begin()
  }

  def stopCrawling() {
    httpClient.getConnectionManager.shutdown()
  }

}


//use startUrl, not domain!!!
class Worker(startUrl: String, val maxThreads: Int, httpClient: HttpClient, dbService: TitanDBService) {

  private val logger = LogManager.getLogger(this.getClass.getName)

  private val linkProvider = new LinkProvider(startUrl, dbService)
  private val linkExtractor = new LinkExtractor
  private var futures = new ListBuffer[Future[List[RawUrlRelation]]]


  private val domain = URLUtils.normalize(URLUtils.getDomainName(startUrl))
  private val filterProcessor = FilterProcessorFactory.get(domain)


  def stop() {}

  def begin() {
    /*
       try to find this link in database
       also, we do not save any links to other domains. So we can traverse graph freely
       if we CAN find link -
            traverse
       if we CAN'T find link
            - save it
            - start traverse


     */

    linkProvider.findOrCreateUrl(startUrl)
    initCrawling()
  }

  //implement statistics object
  private var count = 0


  private def crawlUrl(parentLink: String, filterProcessor: FilterProcessor): Future[List[RawUrlRelation]] = {
    implicit val ec = ExecutionContext.Implicits.global
    val thisFuture = future[List[RawUrlRelation]] {
      var links = ListBuffer[String]()
      try {
        count += 1
        val httpGet = new HttpGet(parentLink)
        logger.info("total crawled: " + count)
        val data = load(httpClient, httpGet, 0, new BasicHttpContext())
        //check mime type
        if (data._1.contains("html")) {
          //clean and normalize
          links = linkExtractor.extractLinks(data._2, parentLink)
          logger.info("links extracted: " + links.length + " from " + parentLink)
        }
      }
      //build urlrelations objects
      val rawUrlRelations = links.map(link => {
        new RawUrlRelation(parentLink, link, Action.None)
      })
      //remove invalid links
      val clearedLinks = URLUtils.clearUrlRelations(startUrl, rawUrlRelations.toList)
      //pass to filter
      val filteredRawUrlRelations = clearedLinks.map(link => {
        val action = filterProcessor.filterUrl(link.to)
        new RawUrlRelation(link.from, link.to, action)
      })
      //remove all links we needn't to crawl
      val linksToProcess = filteredRawUrlRelations.filter(url => {
        if (url.action == Action.Download) {
          true
        } else {
          logger.info("skipped url " + url.to)
          false
        }
      })
      linksToProcess
    }

    thisFuture onComplete {
      // in what thread does this execs?
      case Success(rawLinks) => {
        synchronized {
          dbService.updateUrlStatus(parentLink, UrlStatus.Complete)
          futures -= thisFuture

          rawLinks.foreach(rawLinks => {
            linkProvider.addToExtractedLinks(rawLinks)
          })
          initCrawling()
        }

      }
      case _ => logger.error("some king of shit during crawling " + parentLink)
    }
    thisFuture
  }

  private def initCrawling() {
    var hasUrlsToCrawl = true
    while (futures.length < maxThreads && hasUrlsToCrawl) {
      linkProvider.urlToCrawl() match {
        case None => {
          hasUrlsToCrawl = false // exit from loop
          logger.info("sorry, no links to crawl")
        }
        case Some(url) => {
          dbService.updateUrlStatus(url.location, UrlStatus.InProgress)
          val newF = crawlUrl(url.location, filterProcessor)
          futures += newF
          logger.info("starting future for crawling " + url.location)
        }
      }
    }
  }


  private def load(httpClient: HttpClient, httpGet: HttpGet, id: Int, context: BasicHttpContext): (String, String) = {
    logger.info("connecting to " + httpGet.getURI)
    try {
      val response: HttpResponse = httpClient.execute(httpGet, context)
      val entity: HttpEntity = response.getEntity
      val result = EntityUtils.toString(entity)
      EntityUtils.consume(entity)
      httpGet.abort()
      (entity.getContentType.getValue, result)
    }
    catch {
      case e: Exception => {
        logger.info("error during crawling " + httpGet.getURI, e)
        httpGet.abort()
      }
      ("", "")
    }
  }


}