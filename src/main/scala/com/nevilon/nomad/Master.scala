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

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/14/13
 * Time: 7:51 AM
 */
class Master {

  private val MAX_THREADS = 20
  private val NUM_OF_DOMAINS = 5

  private val httpClient = HttpClientFactory.buildHttpClient(MAX_THREADS * NUM_OF_DOMAINS, MAX_THREADS)


  def startCrawling() {
    //run each in separate thread?
    // or run thread inside crawler?
    val worker = new Worker("http://lenta.ru", MAX_THREADS, httpClient)
    worker.begin()
  }

  def stopCrawling() {
    httpClient.getConnectionManager.shutdown()
  }

}


class Link(url: String, parent: String) {}





class LinkProvider2 {

  private val extractedLinks = new ListBuffer[Link]
  private val linksToCrawl = new ListBuffer[Link]


  def addToExtractedLinks(entity: Link) {
    extractedLinks += entity
  }

  def linkToCrawl(): Option[Link] = {
    if (linksToCrawl.size == 0) {
      val links = loadLinksToCrawl()
      linksToCrawl ++= links
      if (links.size == 0) {
        None
      }
      //load from orientdb
      //check if orientdb is empty!
      Some(linksToCrawl.last)
    } else {
      Some(linksToCrawl.last)
    }
  }


  def saveExtractedLinks() {

  }

  def loadLinksToCrawl(): List[Link] = {
    val list = new ListBuffer[Link]
    list.toList
    // get list from orientdb
  }

}


class Worker(domain: String, val maxThreads: Int, httpClient: HttpClient) {


  private val linkProvider = new LinkProvider(domain)
  private val linkExtractor = new LinkExtractor
  val filterProcessor = FilterProcessorFactory.get(domain)

  private var futures = new ListBuffer[Future[LinkRelation]]

  def stop() {}

  def begin() {

    linkProvider.unvisited += domain
    val future = crawlUrl(domain, filterProcessor)
  }


  private def crawlUrl(link: String, filterProcessor: FilterProcessor): Future[LinkRelation] = {
    implicit val ec = ExecutionContext.Implicits.global

    val urlLoader = future[LinkRelation] {
      var links = ListBuffer[String]()
      try {
        val httpGet = new HttpGet(link)
        val data = load(httpClient, httpGet, 0, new BasicHttpContext())
        //check mime type
        if (data._1.contains("html")) {
          links = linkExtractor.extractLinks(data._2, link)
        }
      } finally {
        //
      }
      (link, links.toList)
    }
    urlLoader onComplete {
      case Success((link, links)) => {
        futures -= urlLoader

        linkProvider.unvisited -= link
        linkProvider.visited += link
        synchronized {

          links.foreach(l => {
            //check new links
            println(filterProcessor.filterUrl(l))
          })

          //add to justCrawled links!
          linkProvider.addNewLinks((link, links))

          initCrawling()
        }

      }
      case _ => println("some kind of shit")
    }
    return urlLoader
  }

  private def initCrawling() {
    while (futures.length < maxThreads) {
      if (linkProvider.unvisited.length > 0) {
        val linkToCrawl = linkProvider.unvisited.last
        linkProvider.unvisited.remove(linkProvider.unvisited.indexOf(linkToCrawl))

        val newF = crawlUrl(linkProvider.unvisited.last, filterProcessor)
        futures += newF
      }
    }
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


}

/*
terminology
  domain - http://lenta.ru - normalized!!!
  Link
    url - url of link
    parent - parent link// but in graph could be many parents!!!

 */




