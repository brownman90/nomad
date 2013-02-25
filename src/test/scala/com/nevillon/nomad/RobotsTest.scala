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
package com.nevillon.nomad

import org.junit.{Assert, Test}
import java.net.URL
import crawlercommons.robots.{RobotUtils, SimpleRobotRulesParser}
import crawlercommons.fetcher.UserAgent


class RobotsTest {


  @Test
  def commonsCrawler() {
    val robotsUrl = new URL("http://www.lenta.ru/robots.txt")
    val parser = new SimpleRobotRulesParser()
    val userAgent = new UserAgent("nomad", "nomad@nevilon.com", "http://www.nevilon.com")
    val fetcher = RobotUtils.createFetcher(userAgent, 1)
    val rules = RobotUtils.getRobotRules(fetcher, parser, robotsUrl)
    //val userAgentStrig = userAgent.getUserAgentString
    Assert.assertTrue(rules.isAllowed("http://lenta.ru/somefile"))
    Assert.assertTrue(rules.isAllowed("http://lenta.ru/bin/somefile"))
  }

}
