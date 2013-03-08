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

import collection.mutable.ListBuffer
import com.nevilon.nomad.crawler.EntityParams
import com.nevilon.nomad.boot.GlobalConfig

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

    fps.addEntityFilter(new GroovyEntityFilter(GlobalConfig.profile.filterFile))
    fps.addEntityFilter(new EndEntityFilter)

    fps.addUrlFilter(new RobotsUrlFilter(domain))
    fps.addUrlFilter(new GroovyUrlFilter(GlobalConfig.profile.filterFile))
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
  val Download, Skip, None= Value
}


abstract class Filter[T] {

  import Action._

  def filter(something: T): Option[Action]

}