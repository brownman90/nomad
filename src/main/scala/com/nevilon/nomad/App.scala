package com.nevilon.nomad

import crawler.Master

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/23/13
 * Time: 11:35 AM
 */
object App {

  def main(args: Array[String]) {
    val master = new Master
    master.startCrawling()
    Thread.sleep(10000000)
  }


}
