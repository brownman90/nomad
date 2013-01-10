package com.nevilon.nomad

import org.apache.http._
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.ConnectionKeepAliveStrategy
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.message.BasicHeaderElementIterator
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.protocol.HTTP
import org.apache.http.protocol.HttpContext
import org.apache.http.util.EntityUtils
import collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration._
import org.jsoup.Jsoup
import scala.util.Success
import java.net.URI

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/6/13
 * Time: 9:40 AM
 * To change this template use File | Settings | File Templates.
 */


object Runner {

  type LinkRelation = (String, List[String])

  val dbService = new DBService

  val httpClient = buildHttpClient()

  val unvisited = ListBuffer[String]()
  // add all but start domain!
  val blacklist = new ListBuffer[String]()
  // extension?
  val visited = new ListBuffer[String]() // visited links?


  val startLink = "http://www.lenta.ru"
  dbService.addDomain(startLink)

  /*
    new
    blacklist
    visited

   */

  val MAX_THREADS = 20
  var futures = new ListBuffer[Future[LinkRelation]]

  def crawlUrl(link: String): Future[LinkRelation] = {
    implicit val ec = ExecutionContext.Implicits.global

    val f = future[LinkRelation] {
      var links = ListBuffer[String]()
      try {
        val httpget = new HttpGet(link)
        val data = load(httpClient, httpget, 0, new BasicHttpContext())
        if (data._1.contains("html")){
          val doc = Jsoup.parse(data._2, link)
          import scala.collection.JavaConversions._
          for (link <- doc.select("a")) {
            val absHref: String = link.attr("abs:href")
            links += absHref
          }
        }
      } finally {
        //   httpClient.getConnectionManager().shutdown()
      }
      (link, links.toList)
    }
    f onComplete {
      case Success((link, links)) => {
        println("futurers: " + futures.length)
        futures-=f
        println("futurers: " + futures.length)

        unvisited -= link
        visited += link
       // println("new links: " + links.length)
        addNewLinks((link, links))
        synchronized{
          while (futures.length<MAX_THREADS){
            if(unvisited.length>0){
              val linkToCrawl = unvisited.last
              unvisited.remove(unvisited.indexOf(linkToCrawl))

              val newF =crawlUrl(unvisited.last)
              futures+=newF
            }
          }
        }

      }
      case _ => println("some kind of shit")
    }
    return f
  }

  def addNewLinks(result: LinkRelation) {
    var cleardLinks = result._2.filter(newLink => (!unvisited.contains(newLink) && !visited.contains(newLink)))
    cleardLinks = cleardLinks.filter(newLink=>{! newLink.contains("@")})
    cleardLinks = cleardLinks.filter(newLink=>{! newLink.trim().isEmpty})
    cleardLinks =  cleardLinks.filter(newLink => {
      try {
        val startDomain = getDomainName(startLink)
        val linkDomain = getDomainName(newLink)
        startDomain.equals(linkDomain)
      }
      catch {
        case e:Exception =>{println(e)}
        false
      }
    })

    synchronized{
      cleardLinks.foreach(url=>{
        dbService.addPage(result._1,url)
      })
    }
    unvisited ++= cleardLinks
   // println("duplicates: " + (result._2.length - cleardLinks.length))
    println("visited: " + visited.length + " " + result._1 + " unvisited: "+unvisited.length)
  }

  def getDomainName(url: String): String = {
    val uri = new URI(url.toLowerCase)
    val domain = uri.getHost()
    //println(url)
    if (domain.startsWith("www.")) {
      return domain.substring(4)
    } else {
      return domain
    }
  }

  def main(args: Array[String]) {
    unvisited += startLink
    val f = crawlUrl(startLink)
    Await.result(f, Duration.Inf)
    Thread.sleep(10000000)
  }


  def startLoading {
    val httpClient = buildHttpClient()

    val start = System.currentTimeMillis()
    try {
      var urisToGet = List(
        "http://www.bing.com",
        "http://hc.apache.org/",
        "http://hc.apache.org/httpcomponents-core-ga/",
        "http://hc.apache.org/httpcomponents-client-ga/",
        "http://svn.apache.org/viewvc/httpcomponents/")

      var items = new ListBuffer[String]()
      for (item <- urisToGet) {
        for (i <- 0 to 250) {
          items += item
        }
      }

      urisToGet = items.toList
      val threads = new Array[GetThread](urisToGet.length)
      for (i <- 0 to threads.length - 1) {
        val httpget = new HttpGet(urisToGet(i))
        threads(i) = new GetThread(httpClient, httpget, i + 1)
      }

      for (i <- 0 to threads.length - 1) {
        threads(i).start()
      }

      for (i <- 0 to threads.length - 1) {
        threads(i).join()
      }

    } finally {
      println(System.currentTimeMillis() - start)
      httpClient.getConnectionManager().shutdown()
    }
  }

  def buildHttpClient(): DefaultHttpClient = {
    val cm: PoolingClientConnectionManager = new PoolingClientConnectionManager
    cm.setMaxTotal(80)
    cm.setDefaultMaxPerRoute(30)
    val httpclient: DefaultHttpClient = new DefaultHttpClient(cm)


    httpclient.setKeepAliveStrategy(new ConnectionKeepAliveStrategy {
      def getKeepAliveDuration(response: HttpResponse, context: HttpContext): Long = {
        val it: HeaderElementIterator = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE))
        while (it.hasNext) {
          val he: HeaderElement = it.nextElement
          val param: String = he.getName
          val value: String = he.getValue
          if (value != null && param.equalsIgnoreCase("timeout")) {
            try {
              return value.toLong * 1000
            }
            catch {
              case ignore: NumberFormatException => {
              }
            }
          }
        }
        val target: HttpHost = context.getAttribute(org.apache.http.protocol.ExecutionContext.HTTP_TARGET_HOST).asInstanceOf[HttpHost]
        if ("www.naughty-server.com".equalsIgnoreCase(target.getHostName)) {
          return 5 * 1000
        }
        else {
          return 30 * 1000
        }
      }
    })
    return httpclient
  }

  /**
   * A thread that performs a GET.
   */
  class GetThread(httpClient: HttpClient, httpget: HttpGet, id: Int) extends Thread {

    /**
     * Executes the GetMethod and prints some status information.
     */
    val context = new BasicHttpContext

    override def run {
      load(httpClient, httpget, id, context)
    }

  }


  def load(httpClient: HttpClient, httpget: HttpGet, id: Int, context: BasicHttpContext): (String,String) = {
    println(id + " - about to get something from " + httpget.getURI)
    try {
      val response: HttpResponse = httpClient.execute(httpget, context)
    //  println(id + " - get executed")
      val entity: HttpEntity = response.getEntity
      val result = EntityUtils.toString(entity)
      EntityUtils.consume(entity)
      httpget.abort()
      return (entity.getContentType.getValue, result)
    }
    catch {
      case e: Exception => {
        httpget.abort
        println(id + " - error: " + e + " " + httpget.getURI)
      }
      return ("","")
    }
  }


}




