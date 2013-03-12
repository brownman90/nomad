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

import com.nevilon.nomad.crawler.EntityParams


class EndCoolFilter[T] extends Filter[T] {

  def filter(arg: T): Option[Action.Action] = Some(Action.Download)

}

/*
class EndFilter extends Filter[String] {
  def filter(url: String): Option[Action.Action] = Some(Action.Download)
}


class EndEntityFilter extends Filter[EntityParams] {
  def filter(entityParams: EntityParams): Option[Action.Action] = Some(Action.Download)
}

class EndDomainFilter extends Filter[String] {
  def filter(domain: String): Option[Action.Action] = Some(Action.Download)
}

*/