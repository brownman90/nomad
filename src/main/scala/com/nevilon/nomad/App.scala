package com.nevilon.nomad

import boot.SeedReader
import crawler.Master
import java.lang.Shutdown

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/23/13
 * Time: 11:35 AM
 */
object App {

  def main(args: Array[String]) {
    val seedsPath = args(0)
    val seedReader = new SeedReader(seedsPath)

    val master = new Master(seedReader.getSeeds)
    sys.ShutdownHookThread {
      master.stop()
      while (!master.isComplete) {
        println("waiting for completion")
        Thread.sleep(5000)
      }

      println("exiting !!!!!!!!!!!!!!!!!")
    }

    master.startCrawling()
    Thread.sleep(10000000)
  }


}



