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
class Carousel[T](val maxThreads: Int, dataProvider: PopProvider) extends Logs {

  private var futures = new ListBuffer[Future[T]]

  private var onStartMethod: (Url) => T = null
  private var onBeforeStart: (Url) => Unit = null
  private var onComplete: (Try[T], Url) => Unit = null


  def stop() {}

  def start() {
    var hasData = true
    while (futures.length < maxThreads && hasData) {
      dataProvider.pop() match {
        case None => {
          hasData = false // exit from loop
          info("sorry, no links to crawl")
        }
        case Some(url) => {
          onBeforeStart(url)
          futures += buildFuture(url,
            ((future: Future[T]) => {
              futures -= future
            })
          )
          info("starting future for crawling " + url.location)
        }
      }
    }
  }


  def buildFuture(url: Url, cleaner: (Future[T]) => Unit): Future[T] = {
    implicit val ec = ExecutionContext.Implicits.global
    val thisFuture = future[T] {
      onStartMethod(url)
    }
    thisFuture.onComplete((data: Try[T]) => ({
      cleaner(thisFuture)
      onComplete(data, url)
    }))
    thisFuture
  }

  def setOnStart(method: (Url) => T) {
    onStartMethod = method
  }

  def setOnBeforeStart(method: (Url) => Unit) {
    onBeforeStart = method
  }

  def setOnComplete(method: (Try[T], Url) => Unit) {
    onComplete = method
  }

  //use either
  def onFailure() {}


}
