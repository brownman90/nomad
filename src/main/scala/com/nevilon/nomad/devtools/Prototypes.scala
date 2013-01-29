package com.nevilon.nomad.devtools

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/29/13
 * Time: 6:29 AM
 */
object Prototypes {

  def timed[T](thunk: => T) = {
    val t1 = System.nanoTime
    val ret = thunk
    val time = System.nanoTime - t1
    println("Executed in: " + time / 1000000.0 + " millisec")
    ret
  }

}
