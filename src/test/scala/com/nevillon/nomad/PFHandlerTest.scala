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

class PFHandlerTest {


  @Test def runTest() {

    val sample = 1 to 10
    val isEven: PartialFunction[Int, String] = {
      case x if x % 2 == 0 => x + " is even"
    }

    // the method collect can use isDefinedAt to select which members to collect
    val evenNumbers = sample collect isEven

    val isOdd: PartialFunction[Int, String] = {
      case x if x % 2 == 1 => x + " is odd"
    }

    // the method orElse allows chaining another partial function to handle
    // input outside the declared domain
    val numbers = sample map (isEven orElse isOdd)

  }

}

