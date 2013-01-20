package com.nevilon.nomad

import collection.mutable.ListBuffer
import concurrent._
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.{HttpEntity, HttpResponse}
import org.apache.http.util.EntityUtils
import scala.util.Success
import collection.mutable
import storage.graph.TitanDBService

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/14/13
 * Time: 7:51 AM
 */

object Exe {
//
//  Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.WARN);
//  Logger.getLogger("httpclient.wire.header").setLevel(Level.WARN);
//  Logger.getLogger("httpclient.wire.content").setLevel(Level.WARN);

 // java.util.logging.Logger.getLogger("org.apache.http.wire").setLevel(java.util.logging.Level.FINEST);
 // java.util.logging.Logger.getLogger("org.apache.http.headers").setLevel(java.util.logging.Level.FINEST);
 // System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
  //System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
 // System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "ERROR");
 // System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "ERROR");
 // System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers", "ERROR");

  def main(args: Array[String]) {
    val master = new Master
    master.startCrawling()
    Thread.sleep(10000000)
  }

}

class Master {

  private val MAX_THREADS = 8
  private val NUM_OF_DOMAINS = 1

  private val httpClient = HttpClientFactory.buildHttpClient(MAX_THREADS * NUM_OF_DOMAINS, MAX_THREADS)

  private val dbService = new TitanDBService(true) //DBService


  def startCrawling() {
    //run each in separate thread?
    // or run thread inside crawler?
    val worker = new Worker("http://www.w3c.org/", MAX_THREADS, httpClient, dbService)
    worker.begin()
  }

  def stopCrawling() {
    httpClient.getConnectionManager.shutdown()
  }

}


class LinkProvider2(domain: String, dbService: TitanDBService) {

  private val extractedLinks = new ListBuffer[RawLinkRelation]
  private val linksToCrawl = new mutable.ArrayStack[Url2]


  /*
    url - normalized form
   */
  def findOrCreateUrl(url: String) {
    /*
       go to service
       do we need to check domains table?
       obviously we need domains table to choose domains for crawling
       maybe we should check(lookup for) link and than use domains table as while list?
       so here we need just find url in urls table and than check if domain is in white list(domains table)

     */
    dbService.getOrCreateUrl(url)
  }

  def addToExtractedLinks(linkRelation: RawLinkRelation) {
    extractedLinks += linkRelation
  }

  def urlToCrawl(): Option[Url2] = {
    if (linksToCrawl.size == 0) {
      flushExtractedLinks()
      val links = loadLinksForCrawling(domain)
      if (links.size == 0) None
      else {
        linksToCrawl ++= links
        Some(linksToCrawl.pop())
      }
    } else {
      if (extractedLinks.length >= 10000) {
        flushExtractedLinks()
      }
      Some(linksToCrawl.pop())
    }
  }


  def updateUrlStatus(url: String, urlStatus: UrlStatus.Value) {
    dbService.updateUrlStatus(url, urlStatus)
  }

  def flushExtractedLinks() {
    this.synchronized {
      dbService.linkUrls(extractedLinks.toList)
      println("flushed: " + extractedLinks.length)
      extractedLinks.clear()
    }
  }

  private def loadLinksForCrawling(startUrl: String): List[Url2] = {
    val bfsLinks = dbService.getBFSLinks(startUrl, 10000)
    bfsLinks.toList

    //    new ListBuffer[Url]().toList


  }

}


//use startUrl, not domain!!!
class Worker(domain: String, val maxThreads: Int, httpClient: HttpClient, dbService: TitanDBService) {


  private val linkProvider = new LinkProvider2(domain, dbService)
  private val linkExtractor = new LinkExtractor
  val filterProcessor = FilterProcessorFactory.get(domain)

  private var futures = new ListBuffer[Future[List[RawLinkRelation]]]

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


    linkProvider.findOrCreateUrl(domain)
    /*
    val urlToCrawl = linkProvider.urlToCrawl()
    urlToCrawl match {
      case None => throw new RuntimeException("No links to crawl!")
      case Some(url) => {
        initCrawling()
        //crawlUrl()
      }
    }
    */
    initCrawling()


    /*
      if linksToCrawl.size=0
        save crawled links
        get links to crawl

     */
    //linkProvider.unvisited += domain
    //val future = crawlUrl(domain, filterProcessor)
  }

  var count = 0


  private def crawlUrl(parentLink: String, filterProcessor: FilterProcessor): Future[List[RawLinkRelation]] = {
    implicit val ec = ExecutionContext.Implicits.global
    //  (startLink, List(links))
    val thisFuture = future[List[RawLinkRelation]] {
      var links = ListBuffer[String]()
      try {
        count += 1
        val httpGet = new HttpGet(parentLink)
        println("get: " + count)
        val data = load(httpClient, httpGet, 0, new BasicHttpContext())
        //check mime type
        if (data._1.contains("html")) {
          //clean and normalize
          links = linkExtractor.extractLinks(data._2, parentLink)
          println("extracted links: " + links.length)
        }
      } finally {
        //
      }

      val start = System.currentTimeMillis()
      dbService.updateUrlStatus(parentLink, UrlStatus.Complete)

      val rawLinks =  links.map(link=>{new RawLinkRelation(parentLink,link)})

      val t = clearLinks(rawLinks.toList)
      println("afert: " + (System.currentTimeMillis() - start))
      t
      //links.map(extractedLink => (parentLink, extractedLink)).toList
    }

    thisFuture onComplete {
      // in what thread does this execs?
      case Success(rawLinks) => {
        val start = System.currentTimeMillis()
        synchronized {
          futures -= thisFuture

          rawLinks.foreach(rawLinks=>{linkProvider.addToExtractedLinks(rawLinks)})
          //linkProvider.flushExtractedLinks()
          initCrawling()
        }

        println("success: " + (System.currentTimeMillis() - start))
      }


      case _ => println("some kind of shit")
    }
    thisFuture
  }

  private def initCrawling() {
    var hasUrlsToCrawl = true
    while (futures.length < maxThreads && hasUrlsToCrawl) {
      // println("futures: " + futures.length)
      // if (linkProvider.unvisited.length > 0) {
      //  val linkToCrawl = linkProvider.unvisited.last
      // linkProvider.unvisited.remove(linkProvider.unvisited.indexOf(linkToCrawl))
      linkProvider.urlToCrawl() match {

        case None => {
          hasUrlsToCrawl = false // exit from loop
          println("sorry, no links to crawl")

          //throw new RuntimeException("No links to crawl!")
        }
        case Some(url) => {
          dbService.updateUrlStatus(url.location, UrlStatus.InProgress)
          val newF = crawlUrl(url.location, filterProcessor)
          futures += newF
          println("starting future for crawling")
        }
      }


    }
    // }

  }


  private def load(httpClient: HttpClient, httpGet: HttpGet, id: Int, context: BasicHttpContext): (String, String) = {
    println(id + " - about to get something from " + httpGet.getURI)
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
        e.printStackTrace()
        httpGet.abort
        println(id + " - error: " + e + " " + httpGet.getURI)
      }
      ("", "")
    }
  }


  def clearLinks(linksToClear: List[RawLinkRelation]): List[RawLinkRelation] = {
    //normalize
    //
    var clearedLinks = List[RawLinkRelation]()
    //remove email links
    clearedLinks = linksToClear.filter(newLink => {
      !newLink.to.contains("@")
    })
    //remove empty links
    clearedLinks = clearedLinks.filter(newLink => {
      !newLink.to.trim().isEmpty
    })
    clearedLinks = clearedLinks.map(newLink => new RawLinkRelation(newLink.from,URLUtils.normalize(newLink.to)))
    clearedLinks = clearedLinks.filter(newLink => {
      !newLink.equals(linksToClear.to)
    })
    //remove links to another domains
    clearedLinks = clearedLinks.filter(newLink => {
      try {
        //accept links from this domain only!
        val startDomain = URLUtils.getDomainName(domain)
        val linkDomain = URLUtils.getDomainName(newLink.to)
        startDomain.equals(linkDomain)
      }
      catch {
        case e: Exception => {
          println(e)
        }
        false
      }
    })
    //remove duplicates
    clearedLinks.distinct
  }

}

/*
terminology
  domain - http://lenta.ru - normalized!!!
  Link
    url - url of link
    parent - parent link// but in graph could be many parents!!!

 */




