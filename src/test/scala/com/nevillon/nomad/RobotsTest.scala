package com.nevillon.nomad

import org.junit.{Assert, Test}
import java.net.URL
import crawlercommons.robots.{RobotUtils, SimpleRobotRulesParser}
import crawlercommons.fetcher.UserAgent

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/12/13
 * Time: 6:22 AM
 */
class RobotsTest {


  @Test
  def commonsCrawler() {
    val robotsUrl = new URL("http://www.lenta.ru/robots.txt")
    val parser = new SimpleRobotRulesParser()
    val userAgent = new UserAgent("nomad", "nomad@nevilon.com", "http://www.nevilon.com")
    val fetcher = RobotUtils.createFetcher(userAgent, 1)
    val rules = RobotUtils.getRobotRules(fetcher, parser, robotsUrl)
    val userAgentStrig = userAgent.getUserAgentString
    println(userAgentStrig)
    Assert.assertTrue(rules.isAllowed("http://lenta.ru/somefile"))
    Assert.assertFalse(rules.isAllowed("http://lenta.ru/bin/somefile"))
  }

}
