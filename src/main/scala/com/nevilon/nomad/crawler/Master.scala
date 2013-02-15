package com.nevilon.nomad.crawler

import collection.mutable.ArrayBuffer
import com.nevilon.nomad.storage.graph.{FileStorage, TitanDBService}
import com.nevilon.nomad.logs.Logs
import collection.mutable
import com.nevilon.nomad.ControlState

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/14/13
 * Time: 7:51 AM
 */

class Master(seeds: List[String]) extends StatisticsPeriodicalPrinter with Logs {

  private var seedsQueue = new mutable.SynchronizedQueue[String]
  seeds.foreach(item => seedsQueue += item)

  private val MAX_THREADS = 10
  private val NUM_OF_WORKERS = 4

  private val httpClient = HttpClientFactory.buildHttpClient(MAX_THREADS * NUM_OF_WORKERS, MAX_THREADS)
  private val dbService = new TitanDBService(true)
  private val workers = new ArrayBuffer[Worker]

  private val fileStorage = new FileStorage()

  def startCrawling() {
    startPrinting()
    info("start workerks")
    loadWatchers()
  }

  private def loadWatchers() {
    while (seedsQueue.nonEmpty && workers.size < NUM_OF_WORKERS && ControlState.canWork) {
      val worker: Worker = new Worker(seedsQueue.dequeue(), MAX_THREADS,
        httpClient, dbService, (worker: Worker) => onCrawlingComplete(worker), fileStorage)
      worker.begin()
      workers += worker
    }
   // ControlState.setCanWork(canWork = false)
  }

  private def shutdown() {
    httpClient.getConnectionManager.shutdown()
  }

  private def onCrawlingComplete(worker: Worker) {
    info("I'm dead! " + worker.startUrl)
    workers -= worker
    if (workers.isEmpty) {
      stopPrinting()
      shutdown()
    }
  }


}