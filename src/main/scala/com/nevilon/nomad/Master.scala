package com.nevilon.nomad

import collection.mutable.ListBuffer
import concurrent._
import com.nevilon.nomad.Types._
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.{HttpEntity, HttpResponse}
import org.apache.http.util.EntityUtils
import scala.util.Success
import collection.mutable
import com.nevilon.nomad.UrlStatus.UrlStatus

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/14/13
 * Time: 7:51 AM
 */

object Exe {

  def main(args: Array[String]) {
    val master = new Master
    master.startCrawling()
    Thread.sleep(1000000)
  }

}

class Master {

  private val MAX_THREADS = 20
  private val NUM_OF_DOMAINS = 5

  private val httpClient = HttpClientFactory.buildHttpClient(MAX_THREADS * NUM_OF_DOMAINS, MAX_THREADS)

  private val dbService = new DBService


  def startCrawling() {
    //run each in separate thread?
    // or run thread inside crawler?
    val worker = new Worker("http://lenta.ru", MAX_THREADS, httpClient, dbService)
    worker.begin()
  }

  def stopCrawling() {
    httpClient.getConnectionManager.shutdown()
  }

}


/*
class Link(url: String, parent: String) {}
*/


class LinkProvider2(domain: String, dbService: DBService) {

  private val extractedLinks = new ListBuffer[LinkRelation]
  private val linksToCrawl = new mutable.ArrayStack[Url]


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

  def addToExtractedLinks(linkRelation: LinkRelation) {
    extractedLinks += linkRelation
  }

  def urlToCrawl(): Option[Url] = {
    if (linksToCrawl.size == 0) {
      //load from orientdb
      //check if orientdb is empty!
      //flush
      flushExtractedLinks()
      val links = loadLinksForCrawling(domain)
      if (links.size == 0) None
      else {
        linksToCrawl ++= links
        Some(linksToCrawl.pop())
      }
    } else {
      Some(linksToCrawl.pop())
    }
  }

  def updateUrlStatus(url: String, urlStatus: UrlStatus) {
    dbService.updateUrlStatus(url, urlStatus)
  }

  def flushExtractedLinks() {
    extractedLinks.foreach(relation => {
      println(relation._1, relation._2)
      dbService.linkUrls(relation._1, relation._2)
    })
    extractedLinks.clear()
  }

  private def loadLinksForCrawling(startUrl: String): List[Url] = {
    val bfsLinks = dbService.getBFSLinks(startUrl, 500)
    bfsLinks.toList
  }

}


//use startUrl, not domain!!!
class Worker(domain: String, val maxThreads: Int, httpClient: HttpClient, dbService: DBService) {


  private val linkProvider = new LinkProvider2(domain, dbService)
  private val linkExtractor = new LinkExtractor
  val filterProcessor = FilterProcessorFactory.get(domain)

  private var futures = new ListBuffer[Future[LinksTree]]

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
    val urlToCrawl = linkProvider.urlToCrawl()
    urlToCrawl match {
      case None => throw new RuntimeException("No links to crawl!")
      case Some(url) => {
        initCrawling()
        //crawlUrl()
      }
    }
    /*
      if linksToCrawl.size=0
        save crawled links
        get links to crawl

     */
    //linkProvider.unvisited += domain
    //val future = crawlUrl(domain, filterProcessor)
  }

  var count = 0


  private def crawlUrl(parentLink: String, filterProcessor: FilterProcessor): Future[LinksTree] = {
    implicit val ec = ExecutionContext.Implicits.global
    //  (startLink, List(links))
    val thisFuture = future[LinksTree] {
      var links = ListBuffer[String]()
      try {
        count+=1
        val httpGet = new HttpGet(parentLink)
        println(count)
        val data = load(httpClient, httpGet, 0, new BasicHttpContext())
        //check mime type
        if (data._1.contains("html")) {
          //clean and normalize
          links = linkExtractor.extractLinks(data._2, parentLink)
        }
      } finally {
        //
      }

      clearLinks(parentLink, links.toList)
      //links.map(extractedLink => (parentLink, extractedLink)).toList
    }

    thisFuture onComplete {
      // in what thread does this execs?
      case Success(relations) => {
        dbService.updateUrlStatus(relations._1, UrlStatus.Complete)
        futures -= thisFuture
        synchronized {
          relations._2.foreach(relation => {
            linkProvider.addToExtractedLinks((relations._1, relation))
            // println(filterProcessor.filterUrl(l))
          })
          initCrawling()
        }
      }
      case _ => println("some kind of shit")
    }
    thisFuture
  }

  private def initCrawling() {
    var hasUrlsToCrawl = true
    while (futures.length < maxThreads && hasUrlsToCrawl) {

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
        httpGet.abort
        println(id + " - error: " + e + " " + httpGet.getURI)
      }
      ("", "")
    }
  }


  def clearLinks(linksToClear: LinksTree): LinksTree = {
    //normalize
    //
    var clearedLinks = linksToClear._2
    //remove email links
    clearedLinks = clearedLinks.filter(newLink => {
      !newLink.contains("@")
    })
    //remove empty links
    clearedLinks = clearedLinks.filter(newLink => {
      !newLink.trim().isEmpty
    })
    clearedLinks = clearedLinks.map(newLink=>URLUtils.normalize(newLink))
    clearedLinks = clearedLinks.filter(newLink => {
      !newLink.equals(linksToClear._1)
    })
    //remove links to another domains
    clearedLinks = clearedLinks.filter(newLink => {
      try {
        //accept links from this domain only!
        val startDomain = URLUtils.getDomainName(domain)
        val linkDomain = URLUtils.getDomainName(newLink)
        startDomain.equals(linkDomain)
      }
      catch {
        case e: Exception => {
          println(e)
        }
        false
      }
    })
    (linksToClear._1, clearedLinks)
  }

}

/*
terminology
  domain - http://lenta.ru - normalized!!!
  Link
    url - url of link
    parent - parent link// but in graph could be many parents!!!

 */




