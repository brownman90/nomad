package com.nevilon.nomad.filter

import collection.mutable.ListBuffer
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
    urlFilterSet.filter(url)
  }

  def filterEntity(entityParams: EntityParams): Action = {
    entityFilterSet.filter(entityParams)
  }


}

class FilterSet[T] {

  protected val filters = new ListBuffer[Filter[T]]

  def addFilter(filter: Filter[T]) {
    filters += filter
  }

  import Action._

  def filter(something: T): Action.Value = {
    var result = Action.None
    for (i <- 0 until filters.length if result == None) {
      filters(i).filter(something) match {
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

  protected var urlFilterSet = new FilterSet[String]
  protected var entityFilterSet = new FilterSet[EntityParams]

}

trait FilterProcessorConstructor extends AbsFilterProcessor {

  def addUrlFilter(urlFilter: Filter[String]) {
    urlFilterSet.addFilter(urlFilter)
  }

  def addEntityFilter(entityFilter: Filter[EntityParams]) {
    entityFilterSet.addFilter(entityFilter)
  }

}


object Action extends Enumeration {

  type Action = Value
  val Download, Skip, None = Value
}


abstract class Filter[T] {

  import Action._

  def filter(something: T): Option[Action]

}