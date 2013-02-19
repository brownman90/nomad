package com.nevilon.nomad.crawler

import com.nevilon.nomad.logs.Logs
import concurrent._
import scala.util.Try
import scala.Some
import collection.mutable

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/10/13
 * Time: 8:29 AM
 */
class Carousel(val maxThreads: Int, dataProvider: PopProvider) extends Logs {

  private var futures = new mutable.HashSet[Future[Unit]] with mutable.SynchronizedSet[Future[Unit]]

  private var onStartMethod: (Url) => Unit = null
  private var onBeforeStart: (Url) => Unit = null
  private var onCrawlingComplete: () => Unit = null

  private var canWork = true

  private val sync = new Object

  private val t = new Thread() {

    override def run() {
      var flag = true

      sync.synchronized {

        while (flag) {

          if (canWork) {
            var hasData = true
            while (futures.size < maxThreads && hasData) {
              dataProvider.pop() match {
                case None => {
                  hasData = false // exit from loop
                  info("sorry, no links to crawl ")
                  if (futures.isEmpty) {
                    onCrawlingComplete()
                  }
                }
                case Some(url) => {
                  onBeforeStart(url)
                  futures += buildFuture(url)
                  info("starting future for crawling " + url.location)
                }
              }
            }
            sync.wait()
          } else if (futures.isEmpty) {
            println("COMPLETE CAR!")
            onCrawlingComplete()
            println("########################################################")
            flag = false
          }else{
            sync.wait()
          }
         // sync.wait()
        }
      }

    }

  }

  t.start()

  def stop() {
    println("canWOrk === false")
    canWork = false
  }

  /*
  def start() {
    synchronized {
      if (canWork) {
        var hasData = true
        while (futures.size < maxThreads && hasData) {
          dataProvider.pop() match {
            case None => {
              hasData = false // exit from loop
              info("sorry, no links to crawl ")
              if (futures.isEmpty) {
                onCrawlingComplete()
              }
            }
            case Some(url) => {
              onBeforeStart(url)
              futures += buildFuture(url)
              info("starting future for crawling " + url.location)
            }
          }
        }
      } else {
        if (futures.isEmpty) {
          onCrawlingComplete()
        }
      }
    }
  }
  */


  private def buildFuture(url: Url): Future[Unit] = {
    implicit val ec = ExecutionContext.Implicits.global
    val thisFuture = future {
      onStartMethod(url)
    }
    thisFuture.onComplete((data: Try[Unit]) => ({
      futures -= thisFuture
      sync.synchronized {
        sync.notify()
      }
      //start()

    }))
    thisFuture
  }

  def setOnStart(method: (Url) => Unit) {
    onStartMethod = method
  }

  def setOnBeforeStart(method: (Url) => Unit) {
    onBeforeStart = method
  }

  def setOnCrawlingComplete(method: () => Unit) {
    onCrawlingComplete = method
  }

}
