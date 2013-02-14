package com.nevilon.nomad.crawler

import java.util.{TimerTask, Timer}
import com.nevilon.nomad.logs.{Logs, Statistics}

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/14/13
 * Time: 8:50 AM
 */
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
