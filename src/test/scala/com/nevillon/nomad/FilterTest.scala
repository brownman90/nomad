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
import collection.mutable
import collection.mutable.ArrayBuffer


class FilterTest {

  @Test
  def splitTest() {
    val items = List(1, 2, 3, 4, 5, 6)

    val itemsWithStatus = new mutable.HashMap[Int, Int]

    items.foreach(i => {
      itemsWithStatus.put(i, 0)
    })

    val t = itemsWithStatus.map({
      case (k, v) => {
        println(v)
        //try to filter - set v to new status
        k -> v
      }
    })
    //t.span

    println(t)
    val list = new ArrayBuffer[(String) => String]


    val mappedItems = items.map(item => {
      if (item % 2 == 0) {
        0
      } else 3

    })
    println(mappedItems)
    val i = oldAndNew(mappedItems, 0)
    println(i)


  }

  @Test
  def iterateTest() {
    for (i <- 1 to 10 if i <5) {
       println(i)
    }
  }

  private def oldAndNew(mappedItems: List[Int], splitValue: Int): (List[Int], List[Int]) = {
    val newItems = mappedItems.filter(_ == splitValue)
    val oldItems = mappedItems.filterNot(_ == splitValue)
    (newItems, oldItems)
  }


}
