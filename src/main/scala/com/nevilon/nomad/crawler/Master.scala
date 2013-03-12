/**
 * Copyright (C) 2012-2013 Vadim Bartko (vadim.bartko@nevilon.com).
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * See file LICENSE.txt for License information.
 */
package com.nevilon.nomad.crawler

import collection.mutable.ArrayBuffer
import com.nevilon.nomad.storage.graph.{SynchronizedDBService, FileStorage}
import com.nevilon.nomad.logs.{Statistics, Logs}
import collection.mutable
import com.nevilon.nomad.boot.GlobalConfig
import java.util.{TimerTask, Timer}

class Master(seeds: List[String]) extends StatisticsPeriodicalPrinter with Logs {

  private var seedsQueue = new mutable.SynchronizedQueue[String]
  seeds.foreach(item => seedsQueue += item)

  private val MAX_THREADS = GlobalConfig.masterConfig.threadsInWorker
  private val NUM_OF_WORKERS = GlobalConfig.masterConfig.workers

  private val workers = new ArrayBuffer[Worker]

  private val dbService = new SynchronizedDBService
  private val domainInjector = new DomainInjector(dbService)
  seeds.foreach(seed => domainInjector.inject(seed))

  private val contentSaver = new ContentSaver(new FileStorage(GlobalConfig.mongoDBConfig))

  def startCrawling() {
    val domainsIt = buildDomainIterator(DomainStatus.IN_PROGRESS)
    domainsIt.foreach(domain => {
      dbService.updateDomain(domain.get.updateStatus(DomainStatus.NEW))
    })
    startPrinting()
    info("start workers")
    loadWorkers()
    new LoadNewDomainsTimer().startPrinting()
  }


  private def buildDomainIterator(domainStatus: DomainStatus.Value) = {
    def hasMore(domain: Option[Domain]): Boolean = {
      domain match {
        case None => false
        case _ => true
      }
    }
    Iterator.continually(dbService.getDomainWithStatus(domainStatus)).takeWhile(domain => hasMore(domain))
  }

  private def loadWorkers() = synchronized {
    val domainIt = buildDomainIterator(DomainStatus.NEW)
    for {
      domainOpt: Option[Domain] <- domainIt
      domain = domainOpt.get
      if (workers.size < NUM_OF_WORKERS)
    } {
      dbService.updateDomain(domain.updateStatus(DomainStatus.IN_PROGRESS))
      val urlsToCrawl = dbService.getLinksToCrawl(domain, 1)
      if (urlsToCrawl.nonEmpty) {
        dbService.updateDomain(domain.updateStatus(DomainStatus.IN_PROGRESS))
        val worker: Worker = new Worker(domain, urlsToCrawl.last.location, MAX_THREADS,
          dbService, contentSaver, (worker: Worker) => onWorkerStop(worker))
        workers += worker
        dbService.updateDomain(domain.updateStatus(DomainStatus.IN_PROGRESS))
      } else info("none links to crawl for domain " + domain.name)
    }
  }


  def isMasterStopped = workers.isEmpty

  def sendStopCommand() {
    info("send stop command, workers: " + workers.size)
    workers.foreach(w => w.stop(true))
  }

  private def onWorkerStop(worker: Worker) {
    info("I'm dead! " + worker.startUrl)
    worker.stop(false) //wtf?
    dbService.updateDomain(worker.domain.updateStatus(DomainStatus.COMPLETE))
    workers -= worker
    loadWorkers()
    if (workers.isEmpty) {
      stopPrinting()
    }
  }


  class LoadNewDomainsTimer {

    private val timer = new Timer()

    private val timerTask = new TimerTask {
      def run() {
        val it = buildDomainIterator(DomainStatus.NEW)
//        it.foreach(d => println(d.get.name))

        loadWorkers()
      }
    }


    def startPrinting() {
      timer.schedule(timerTask, 0, 30000)
    }

    def stopPrinting() {
      timer.cancel()
    }

  }


}