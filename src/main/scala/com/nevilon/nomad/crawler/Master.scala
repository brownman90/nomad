package com.nevilon.nomad.crawler

import collection.mutable.ArrayBuffer
import com.nevilon.nomad.storage.graph.{FileStorage, TitanDBService}
import com.nevilon.nomad.logs.Logs
import collection.mutable
import com.nevilon.nomad.boot.GlobalConfig

/**
 * Created with IntelliJ  IDEA.
 * User: hudvin
 * Date: 1/14/13
 * Time: 7:51 AM
 */

class Master(seeds: List[String]) extends StatisticsPeriodicalPrinter with Logs {

  private var seedsQueue = new mutable.SynchronizedQueue[String]
  seeds.foreach(item => seedsQueue += item)

  private val MAX_THREADS = GlobalConfig.masterConfig.threadsInWorker
  private val NUM_OF_WORKERS = GlobalConfig.masterConfig.workers

  private val dbService = new TitanDBService(true)
  private val workers = new ArrayBuffer[Worker]

  private val fileStorage = new FileStorage(GlobalConfig.mongoDBConfig)

  def startCrawling() {
    startPrinting()
    info("start workers")
    loadWatchers()
  }

  private def loadWatchers() {
    while (seedsQueue.nonEmpty && workers.size < NUM_OF_WORKERS) {
      // add flag for stop
      val worker: Worker = new Worker(seedsQueue.dequeue(), MAX_THREADS,
        dbService, (worker: Worker) => onCrawlingComplete(worker), fileStorage)
      workers += worker
    }
  }


  def isComplete: Boolean = {
    workers.foreach(w => println(w.startUrl))
    workers.isEmpty
  }

  def stop() {
    info("send stop command, workers: " + workers.size)
    workers.foreach(w => w.stop(true))
  }

  private def onCrawlingComplete(worker: Worker) {
    info("I'm dead! " + worker.startUrl)
    worker.stop(false)
    workers -= worker
    if (workers.isEmpty) {
      stopPrinting()
    }
  }


}