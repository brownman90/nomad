package com.nevillon.nomad

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 3/5/13
 * Time: 12:57 PM
 */


import org.junit.{Assert, Test}
import com.nevilon.nomad.crawler.URLUtils


class SyncTraitTest {

  @Test def testSyncTrait() {
    val c = new NeedToSync with SyncTrait
    c.someMethod
  }

}


class NeedToSync extends BasicTrait {

  override def someMethod() {
    println("need to sync")
  }

}

trait BasicTrait {

  def someMethod()

}

trait SyncTrait extends BasicTrait {

  abstract override def someMethod = synchronized {
    println("sync")
    super.someMethod
  }

}



