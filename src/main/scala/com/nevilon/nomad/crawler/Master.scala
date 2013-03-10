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
import com.nevilon.nomad.storage.graph.{APIFacade, FileStorage, TitanDBService}
import com.nevilon.nomad.logs.Logs
import collection.mutable
import com.nevilon.nomad.boot.GlobalConfig

class Master(seeds: List[String]) extends StatisticsPeriodicalPrinter with Logs {

  private var seedsQueue = new mutable.SynchronizedQueue[String]
  seeds.foreach(item => seedsQueue += item)

  private val MAX_THREADS = GlobalConfig.masterConfig.threadsInWorker
  private val NUM_OF_WORKERS = GlobalConfig.masterConfig.workers

  private val apiFacade = new APIFacade
  private val workers = new ArrayBuffer[Worker]

  private val domainInjector = new DomainInjector(apiFacade)
  seeds.foreach(seed => domainInjector.inject(seed))

  def startCrawling() {
    //clear statuses for domains and urls
    startPrinting()
    info("start workers")
    loadWorkers()
  }

  private def loadWorkers() {
    val domainsIt = apiFacade.getDomainWithStatus(DomainStatus.NEW)
    while (domainsIt.nonEmpty && workers.size < NUM_OF_WORKERS) {
      // add flag for stop
      val domain = domainsIt.next()
      val urlsToCrawl = apiFacade.getLinksToCrawl(domain, 1)
      if (urlsToCrawl.nonEmpty) {
        apiFacade.updateDomain(domain.updateStatus(DomainStatus.IN_PROGRESS))
        val worker: Worker = new Worker(domain, urlsToCrawl.last.location, MAX_THREADS,
          apiFacade, (worker: Worker) => onCrawlingComplete(worker))
        workers += worker
      } else {
        info("none links to crawl for domain " + domain.name)
      }
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
    apiFacade.updateDomain(worker.domain.updateStatus(DomainStatus.COMPLETE))
    workers -= worker
    loadWorkers()
    if (workers.isEmpty) {
      stopPrinting()
    }
  }


}