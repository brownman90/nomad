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
package com.nevilon.nomad.boot

import java.lang.Shutdown
import com.typesafe.config.ConfigFactory
import com.nevilon.nomad.logs.Logs
import com.nevilon.nomad.crawler.Master

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

    /*
    sys.ShutdownHookThread {
      master.stop()
      while (!master.isComplete) {
        info("waiting for completion")
        Thread.sleep(5000)
      }

      info("done")
    }
    */

    master.startCrawling()
  }


}


