package com.nevilon.nomad

import boot.SeedReader
import crawler.Master
import java.lang.Shutdown
import logs.Logs

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/23/13
 * Time: 11:35 AM
 */
object App extends  Logs{

  def main(args: Array[String]) {
    val seedsPath = args(0)
    val seedReader = new SeedReader(seedsPath)

    val master = new Master(seedReader.getSeeds)

    sys.ShutdownHookThread {
      master.stop()
      while (!master.isComplete) {
        info("waiting for completion")
        Thread.sleep(5000)
      }

      info("done")
    }

    master.startCrawling()
  }


}

