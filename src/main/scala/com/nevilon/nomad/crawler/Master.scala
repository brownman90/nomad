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
import com.nevilon.nomad.storage.graph.{FileStorage, TitanDBService}
import com.nevilon.nomad.filter.{Action, FilterProcessor, FilterProcessorFactory}
import javax.activation.MimeType
import java.io.{ByteArrayInputStream, File, FileOutputStream, InputStream}
import org.apache.commons.io.FileUtils
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.commons.httpclient.util.URIUtil
import annotation.target
import org.specs2.internal.scalaz.concurrent.Actor
import java.util.{Timer, TimerTask}
import com.nevilon.nomad.logs.{Logs, Tabulator, Statistics, CounterGroup}

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/14/13
 * Time: 7:51 AM
 */

class Master extends Logs {

  //add delay?
  //headers like in browser
  private val MAX_THREADS = 5
  private val NUM_OF_DOMAINS = 1

  private val httpClient = HttpClientFactory.buildHttpClient(MAX_THREADS * NUM_OF_DOMAINS, MAX_THREADS)
  private val dbService = new TitanDBService(true)

  private val timerTask = new TimerTask {
    def run() {
      info("\n" + Statistics.buildStatisticsTable())
    }
  }

  private val timer = new Timer()

  def startCrawling() {
    startTimer()
    info("start workerks")
    val worker = new Worker("http://nlp.stanford.edu/", MAX_THREADS, httpClient, dbService, onCrawlingComplete)
    worker.begin()
  }

  def stopCrawling() {
    httpClient.getConnectionManager.shutdown()
  }

  def onCrawlingComplete() {
    info("I'm dead!")
    stopTimer()
  }


  def startTimer() {
    timer.schedule(timerTask, 0, 5000)
  }

  def stopTimer() {
    timer.cancel()
  }


}


