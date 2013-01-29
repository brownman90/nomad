package com.nevilon.nomad.filter

import collection.mutable.ListBuffer
import java.net.URL
import crawlercommons.robots.{RobotUtils, SimpleRobotRulesParser}
import crawlercommons.fetcher.UserAgent
import com.nevilon.nomad.crawler.EntityParams

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

    fps.addEntityFilter(new GroovyEntityFilter)
    fps.addEntityFilter(new EndEntityFilter)



    fps.addUrlFilter(new RobotsUrlFilter(domain))
    fps.addUrlFilter(new GroovyUrlFilter)
    fps.addUrlFilter(new EndFilter)
    fps
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

  //sorry for copypast, I need some additional time to create nice solution
  def filterEntity(entityParams: EntityParams): Action = {
    var result = Action.None
    for (i <- 0 until entityFilters.length if result == None) {
      entityFilters(i).filter(entityParams) match {
        case scala.None => {}
        case Some(action) => {
          result = action
        }
      }
    }
    result
  }


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

  import Action._

  def filter(entityParams: EntityParams): Option[Action]

}

abstract class Filter {}






