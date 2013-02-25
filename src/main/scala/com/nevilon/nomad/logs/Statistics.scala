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
package com.nevilon.nomad.logs

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import org.apache.commons.lang.builder.{HashCodeBuilder, EqualsBuilder}
import scala._



object Statistics {

  private val groups = new mutable.HashSet[CounterGroup]

  def createCounterGroup(name: String): CounterGroup = {
    val counterGroup = new CounterGroup(name)
    require(!groups.contains(counterGroup))
    groups += counterGroup
    counterGroup
  }


  def remove(counterGroup: CounterGroup) {
    groups -= counterGroup
  }

  def buildStatisticsTable(): String = {
    if (groups.nonEmpty) {
      val rows = new ListBuffer[List[String]]
      rows += List("name", "value")
      groups.foreach(g => {
        rows += List("[url]", g.url)
        g.listCounters().foreach(c => {
          rows += List(c.name, c.getValue.toString)
        })
      })
      Tabulator.format(rows)
    } else {
      "<no counter groups avaliable>"
    }
  }
}


class Counter(val name: String) {

  private var value = 0

  def inc() {
    value += 1
  }

  def getValue: Int = value


  override def equals(obj: Any): Boolean = {
    if (obj.isInstanceOf[Counter]) {
      val other = obj.asInstanceOf[Counter]
      new EqualsBuilder()
        .append(name, other.name)
        .isEquals
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    new HashCodeBuilder()
      .append(name)
      .toHashCode
  }

  override def toString: String = name + " : " + value
}

class CounterGroup(val url: String) {

  private val counters = new mutable.HashMap[String, Counter]()

  def listCounters(): List[Counter] = {
    counters.values.toList
  }

  def createCounter(name: String): Counter = {
    require(!counters.contains(name))
    counters.put(name, new Counter(name))
    counters.get(name).get
  }

  def deleteCounter(name: String) {
    require(counters.contains(name))
    counters.remove(name)
  }


  override def equals(obj: Any): Boolean = {
    if (obj.isInstanceOf[CounterGroup]) {
      val other = obj.asInstanceOf[CounterGroup]
      new EqualsBuilder()
        .append(url, other.url)
        .isEquals
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    new HashCodeBuilder()
      .append(url)
      .toHashCode
  }

}