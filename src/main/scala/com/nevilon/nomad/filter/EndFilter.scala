package com.nevilon.nomad.filter

import com.nevilon.nomad.crawler.EntityParams

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/27/13
 * Time: 8:12 AM
 */
class EndFilter extends Filter[String] {
  def filter(url: String): Option[Action.Action] = Some(Action.Download)
}


class EndEntityFilter extends Filter[EntityParams] {
  def filter(entityParams: EntityParams): Option[Action.Action] = Some(Action.Download)
}

