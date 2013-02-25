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

import java.util.{TimerTask, Timer}
import com.nevilon.nomad.logs.{Logs, Statistics}


trait StatisticsPeriodicalPrinter extends Logs {

  private val timer = new Timer()

  private val timerTask = new TimerTask {
    def run() {
      info("\n" + Statistics.buildStatisticsTable())
    }
  }


  def startPrinting() {
    timer.schedule(timerTask, 0, 5000)
  }

  def stopPrinting() {
    timer.cancel()
  }

}
