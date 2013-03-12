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
package com.nevilon.nomad.filter

import java.net.URL
import crawlercommons.robots.{RobotUtils, SimpleRobotRulesParser}
import crawlercommons.fetcher.UserAgent
import com.nevilon.nomad.crawler.UserAgentProvider

class RobotsUrlFilter(domain: String) extends Filter[String] {

  private val ROBOTS_TXT_LOCATION = "/robots.txt"

  private val robotsUrl = new URL(domain + ROBOTS_TXT_LOCATION)
  private val parser = new SimpleRobotRulesParser()
  private val userAgent = UserAgentProvider.getUserAgent()
  private val fetcher = RobotUtils.createFetcher(userAgent, 1)
  private val rules = RobotUtils.getRobotRules(fetcher, parser, robotsUrl)


  import Action._

  override def filter(url: String): Option[Action] = {
    if (rules.isAllowed(url)) scala.None else Some(Action.Skip)
  }
}
