package com.nevilon.nomad

import org.apache.http._
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.util.EntityUtils
import collection.mutable.ListBuffer
import scala.concurrent._
import duration.Duration
import scala.util.Success
import com.nevilon.nomad.Types.LinkRelation
import com.orientechnologies.orient.core.config.OGlobalConfiguration
import java.util.UUID
import java.net.URL
import crawlercommons.robots.{RobotUtils, SimpleRobotRulesParser}
import crawlercommons.fetcher.UserAgent

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/6/13
 * Time: 9:40 AM
 */


object Runner {

  val httpClient = HttpClientFactory.buildHttpClient()


  val startLink = "http://lenta.ru"
  val linkProvider = new LinkProvider(startLink)
  val linkExtractor = new LinkExtractor


  val MAX_THREADS = 20
  var futures = new ListBuffer[Future[LinkRelation]]

  def crawlUrl(link: String): Future[LinkRelation] = {
    implicit val ec = ExecutionContext.Implicits.global

    val f = future[LinkRelation] {
      var links = ListBuffer[String]()
      try {
        val httpget = new HttpGet(link)
        val data = load(httpClient, httpget, 0, new BasicHttpContext())
        if (data._1.contains("html")) {
          links = linkExtractor.extractLinks(data._2, link)
        }
      } finally {
        //   httpClient.getConnectionManager().shutdown()
      }
      (link, links.toList)
    }
    f onComplete {
      case Success((link, links)) => {
        println("futurers: " + futures.length)
        futures -= f
        println("futurers: " + futures.length)

        linkProvider.unvisited -= link
        linkProvider.visited += link
        // println("new links: " + links.length)
        synchronized {
          linkProvider.addNewLinks((link, links))

          while (futures.length < MAX_THREADS) {
            if (linkProvider.unvisited.length > 0) {
              val linkToCrawl = linkProvider.unvisited.last
              linkProvider.unvisited.remove(linkProvider.unvisited.indexOf(linkToCrawl))

              val newF = crawlUrl(linkProvider.unvisited.last)
              futures += newF
            }
          }
        }

      }
      case _ => println("some kind of shit")
    }
    return f
  }


  def main(args: Array[String]) {


  //  OGlobalConfiguration.TX_USE_LOG.setValue(false)

   // OGlobalConfiguration.MVRBTREE_NODE_PAGE_SIZE.setValue(4096)

    var start = System.currentTimeMillis()
    for (i <- 1 to 500000000) {

      linkProvider.addPage(i.toString, UUID.randomUUID().toString)
      if(i % 10000 == 0){
        println(i + " # "+(System.currentTimeMillis()-start))
        start = System.currentTimeMillis()
      }

      // linkProvider.addDomain(System.currentTimeMillis().toString)
    }
    println(System.currentTimeMillis() - start)



    //linkProvider.unvisited += startLink
   // val f = crawlUrl(startLink)
   // Await.result(f, Duration.Inf)
   // Thread.sleep(10000000)
  }


  def load(httpClient: HttpClient, httpGet: HttpGet, id: Int, context: BasicHttpContext): (String, String) = {
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