package com.nevilon.nomad.filter

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

  /*
    build something like filter function?
    use pipeline?
    pass to next everything not processed by current filter

   */

  //this is domain, not start url!!!!
  def get(domain: String): FilterProcessor = {
    class FilterProcessorWithConstructor extends FilterProcessor with FilterProcessorConstructor
    val fps = new FilterProcessorWithConstructor

    //fps.addEntityFilter()

    fps.addUrlFilter(new RobotsUrlFilter(domain))
    fps.addUrlFilter(new EndFilter)
    fps
  }

}


class EndFilter extends UrlFilter {
  def filter(url: String): Option[Action.Action] = Some(Action.Download)
}


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
    if (rules.isAllowed(url)) Some(Action.Download) else Some(Action.Skip)
  }
}


class FilterProcessor extends AbsFilterProcessor {

  import Action._

  def filterUrl(url: String): Action = {
    //add defaultEmptyFilter = always returns DOWNLOAD!
    var result = Action.None
    for (i <- 0 until urlFilters.length if result == None) {
      urlFilters(i).filter(url) match {
        case scala.None => {}
        case Some(action) => {
          result = action
        }
      }
    }
    result
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
  val Download, Skip, None = Value
}


trait UrlFilter extends Filter {

  import Action._

  def filter(url: String): Option[Action]

}

trait EntityFilter extends Filter {

  def filter()

}

abstract class Filter {}
