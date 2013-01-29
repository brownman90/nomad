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
import javax.activation.MimeType
import java.io.{File, FileOutputStream, InputStream}
import org.apache.commons.io.FileUtils

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/14/13
 * Time: 7:51 AM
 */

class Master {

  private val logger = LogManager.getLogger(this.getClass.getName)

  //add delay?
  //headers like in browser
  private val MAX_THREADS = 7
  private val NUM_OF_DOMAINS = 1

  private val httpClient = HttpClientFactory.buildHttpClient(MAX_THREADS * NUM_OF_DOMAINS, MAX_THREADS)
  private val dbService = new TitanDBService(true) //DBService


  def startCrawling() {
    //run each in separate thread?
    // or run thread inside crawler?
    logger.info("starting workerks")
    //
    val worker = new Worker("http://researcher.watson.ibm.com/", MAX_THREADS, httpClient, dbService)
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


  private var count = 0


  private val saver = (inputStream: InputStream) => {

  }
  private val filter = (entityParams: EntityParams) => {
    filterProcessor.filterEntity(entityParams)
  }


  private def buildEntityParams(httpEntity: HttpEntity, url: String): EntityParams = {
    val mimeType = new MimeType(httpEntity.getContentType.getValue)
    val entityParams = new EntityParams(httpEntity.getContentLength, url, mimeType)
    entityParams
  }


  private def download(url: String): List[String] = {
    var links = ListBuffer[String]()
    val httpGet = new HttpGet(url)

    logger.info("connecting to " + url)
    try {
      val response: HttpResponse = httpClient.execute(httpGet, new BasicHttpContext()) //what is context?
      val entity: HttpEntity = response.getEntity
      val entityParams = buildEntityParams(entity, url)
      if (filterProcessor.filterEntity(entityParams) == Action.Download) {
        //accept
        //filter - SKIP or DOWNLOAD
        //check entity type
        if (entityParams.mimeType.getSubType.contains("html")) {
          links = linkExtractor.extractLinks(EntityUtils.toString(entity), url)
          logger.info("links extracted: " + links.length + " from " + url)
          //extract links
          //save
        } else {


          val path = url.replaceAll("/", "_")
          val out = new FileOutputStream(new File("/tmp/cons/" + path))

          Iterator
            .continually(entity.getContent.read)
            .takeWhile(-1 !=)
            .foreach(out.write)

          out.close()
          //save
        }
      } else {
        logger.info("skip " + url)
        //???
      }
      //how to skip current fetch?
      EntityUtils.consume(entity)
      httpGet.abort()
    }
    catch {
      case e: Exception => {
        logger.info("error during crawling " + url, e)
        httpGet.abort()
      }
    }

    count += 1

    logger.info("total crawled: " + count)
    links.toList
  }

  private def crawlUrl(parentLink: String, filterProcessor: FilterProcessor): Future[List[RawUrlRelation]] = {
    implicit val ec = ExecutionContext.Implicits.global
    val thisFuture = future[List[RawUrlRelation]] {
      val links = download(parentLink)
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
          rawLinks.foreach(linkProvider.addToExtractedLinks(_))
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

}

class EntityParams(val size: Long, val url: String, val mimeType: MimeType)

