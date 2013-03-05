package com.nevillon.nomad

import org.junit.{Assert, Test}

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 3/2/13
 * Time: 5:00 PM
 */
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