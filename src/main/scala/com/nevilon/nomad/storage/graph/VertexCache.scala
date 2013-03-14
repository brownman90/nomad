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
package com.nevilon.nomad.storage.graph

import com.tinkerpop.blueprints.Vertex
import collection.mutable


class Cache[T](load: (T) => Option[Vertex], create: (T) => Option[Vertex]) {

  private val cache = new mutable.HashMap[T, Vertex]

  def getOrElse(key: T): Option[Vertex] = {
    cache.get(key) match {
      case None => {
        load(key) match {
          case None => create(key)
          case Some(v) => {
            cache.put(key, v)
            Some(v)
          }
        }
      }
      case Some(v) => {
        Some(v)
      }
    }
  }

}
