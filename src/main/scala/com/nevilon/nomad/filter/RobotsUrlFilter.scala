package com.nevilon.nomad.filter

import java.net.URL
import crawlercommons.robots.{RobotUtils, SimpleRobotRulesParser}
import crawlercommons.fetcher.UserAgent

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/27/13
 * Time: 8:11 AM
 */
class RobotsUrlFilter(domain: String) extends UrlFilter {

  //set the same user agent for http client
  object RobotsConfig {

    val ROBOTS_TXT = "/robots.txt"
    val CRAWLER_NAME = "nomad"
    val CRAWLER_EMAIL = "homad@nevilon.com"
    val CRAWLER_PAGE = "http://www.nevilon.com"

  }

  private val robotsUrl = new URL(domain + RobotsConfig.ROBOTS_TXT)
  private val parser = new SimpleRobotRulesParser()
  private val userAgent = new UserAgent(RobotsConfig.CRAWLER_NAME, RobotsConfig.CRAWLER_EMAIL, RobotsConfig.CRAWLER_PAGE)
  private val fetcher = RobotUtils.createFetcher(userAgent, 1)
  private val rules = RobotUtils.getRobotRules(fetcher, parser, robotsUrl)


  import Action._

  override def filter(url: String): Option[Action] = {
    if (rules.isAllowed(url)) scala.None  else Some(Action.Skip)
  }
}
