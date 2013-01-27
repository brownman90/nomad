package com.nevillon.nomad

import org.junit.{Assert, Test}
import collection.mutable
import collection.mutable.ArrayBuffer

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/24/13
 * Time: 11:28 AM
 */
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
