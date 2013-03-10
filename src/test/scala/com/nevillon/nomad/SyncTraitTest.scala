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


import org.junit.Test


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



