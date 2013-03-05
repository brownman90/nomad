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
package com.nevilon.nomad.devtools


object Prototypes {

  def timed[T](thunk: => T, name: String) = {
    val t1 = System.nanoTime
    val ret = thunk
    val time = System.nanoTime - t1

    println(name + " executed in: " + time / 1000000.0 + " millisec")
    ret
  }

}
