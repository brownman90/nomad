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
package com.nevillon.nomad

import org.junit.{Assert, Test}

class TraitTest {

  @Test def test() {
    val s = new Service with Con {
     // override lazy val t = "cccc"

      override def getString() = "yyyyyyyy"
    }
  }

}


class Service[T] {
  self: Con =>

  //println(self.t.length)
  println(self.getString())

}


trait Con {

 // lazy val t: String = null

  def getString():String

}

trait ExtCon extends Con {


}