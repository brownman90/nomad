package com.nevilon.nomad.boot

import java.lang.Shutdown
import com.typesafe.config.ConfigFactory
import com.nevilon.nomad.logs.Logs
import com.nevilon.nomad.crawler.Master

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/23/13
 * Time: 11:35 AM
 */
object App extends Logs {

  def main(args: Array[String]) {
    if (args.isEmpty) {
      throw new Error("missing arguments")
    }
    val profilePath = args(0)
    val profile = new Profile(profilePath)

    val seedReader = new SeedReader(profile.seedFile)
    GlobalConfig.load(profile.appConfFile)
    GlobalConfig.profile = profile

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


