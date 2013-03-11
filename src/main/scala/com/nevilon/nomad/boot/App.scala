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

import com.nevilon.nomad.logs.Logs
import com.nevilon.nomad.crawler.Master
import java.io.File

object App extends Logs {

  private case class Config(path: String = "")

  def main(args: Array[String]) {
    val parser = new scopt.immutable.OptionParser[Config]("nomad", "0.4.x") {
      def options = Seq(
        arg("<path>", "path to profile dir, for example profiles/template/") {
          (v: String, c: Config) => c.copy(path = v)
        }
      )
    }
    parser.parse(args, Config()) map {
      config =>
        GlobalConfig.loadProfile(config.path)
        val seedReader = new SeedReader(new File(GlobalConfig.appConfig.seedFile))
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

      // do stuff
    } getOrElse {
      // arguments are bad, usage message will have been displayed
    }
  }


}


