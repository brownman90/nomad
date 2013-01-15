package com.nevilon.nomad

import collection.mutable.ListBuffer
import java.net.URL
import crawlercommons.robots.{RobotUtils, SimpleRobotRulesParser}
import crawlercommons.fetcher.UserAgent

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/14/13
 * Time: 4:10 AM
 */
object FilterProcessorFactory {

  def get(domain: String): FilterProcessor = {
    class FilterProcessorWithConstructor extends FilterProcessor with FilterProcessorConstructor
    val fps = new FilterProcessorWithConstructor

    //fps.addEntityFilter()

    fps.addUrlFilter(new RobotsUrlFilter(domain))
    fps
  }

}


class RobotsUrlFilter(domain: String) extends UrlFilter {

  //set the same user agent for http client
  object RobotsConfig {

    val ROBOTS_TXT = "/robots.txt"
    val CRAWLER_NAME = "nomad"
    val CRAWLER_EMAIL = "homad@nevilon.com"
    val CRAWLER_PAGE = "http://www.nevilon.com"

  }

  val robotsUrl = new URL(domain + RobotsConfig.ROBOTS_TXT)
  val parser = new SimpleRobotRulesParser()
  val userAgent = new UserAgent(RobotsConfig.CRAWLER_NAME, RobotsConfig.CRAWLER_EMAIL, RobotsConfig.CRAWLER_PAGE)
  val fetcher = RobotUtils.createFetcher(userAgent, 1)
  val rules = RobotUtils.getRobotRules(fetcher, parser, robotsUrl)


  import Action._

  override def filter(url: String): Action = {
    if (rules.isAllowed(url)) {
      Action.Download
    } else {
      Action.Skip
    }
  }
}


class FilterProcessor extends AbsFilterProcessor {

  import Action._

  def filterUrl(url: String):List[Action]= {
    var results = new ListBuffer[Action]
    urlFilters.foreach(urlFilter => {
      results+=urlFilter.filter(url)
    })
    results.toList
  }

  def filterEntity() {}


}

class AbsFilterProcessor {

  protected var urlFilters = new ListBuffer[UrlFilter]()
  protected var entityFilters = new ListBuffer[EntityFilter]()


}

trait FilterProcessorConstructor extends AbsFilterProcessor {

  def addUrlFilter(urlFilter: UrlFilter) {
    urlFilters += urlFilter
  }

  def addEntityFilter(entityFilter: EntityFilter) {
    entityFilters += entityFilter
  }

}


object Action extends Enumeration {
  type Action = Value
  val Download, Skip = Value
}


trait UrlFilter extends Filter {

  import Action._

  def filter(url: String): Action

}

trait EntityFilter extends Filter {

  def filter()

}

abstract class Filter {}
