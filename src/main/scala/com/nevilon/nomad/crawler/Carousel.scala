package com.nevilon.nomad.crawler

import com.nevilon.nomad.logs.Logs
import collection.mutable.ListBuffer
import concurrent._
import scala.util.Try
import scala.Some

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/10/13
 * Time: 8:29 AM
 */
class Carousel(val maxThreads: Int, dataProvider: PopProvider) extends Logs {


  private var futures = new ListBuffer[Future[Unit]]

  private var onStartMethod: (Url) => Unit = null
  private var onBeforeStart: (Url) => Unit = null

  def stop() {}

  def start() {
    this.synchronized {
      var hasData = true
      while (futures.length < maxThreads && hasData) {
        dataProvider.pop() match {
          case None => {
            hasData = false // exit from loop
            info("sorry, no links to crawl")
          }
          case Some(url) => {
            onBeforeStart(url)
            futures += buildFuture(url)
            info("starting future for crawling " + url.location)
          }
        }
      }
    }
  }


  private def buildFuture(url: Url): Future[Unit] = {
    implicit val ec = ExecutionContext.Implicits.global
    val thisFuture = future {
      onStartMethod(url)
    }
    thisFuture.onComplete((data: Try[Unit]) => ({
      this.synchronized {
        futures -= thisFuture
      }
    }))
    thisFuture
  }

  def setOnStart(method: (Url) => Unit) {
    onStartMethod = method
  }

  def setOnBeforeStart(method: (Url) => Unit) {
    onBeforeStart = method
  }

}
