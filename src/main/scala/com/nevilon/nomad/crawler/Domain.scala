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
package com.nevilon.nomad.crawler

import org.apache.commons.lang.builder.{EqualsBuilder, HashCodeBuilder}

class Domain(val name: String, val status: DomainStatus.Value) {

  def updateStatus(newStatus: DomainStatus.Value): Domain = new Domain(name, newStatus)

  override def equals(obj: Any): Boolean = {
    if (obj.isInstanceOf[Domain]) {
      val other = obj.asInstanceOf[Domain]
      new EqualsBuilder()
        .append(name, other.name)
        .isEquals
    } else false
  }

  override def hashCode(): Int = {
    new HashCodeBuilder()
      .append(name)
      .toHashCode
  }

}

class DomainStatus

object DomainStatus extends Enumeration {

  val IN_PROGRESS = Value("IN_PROGRESS")
  val COMPLETE = Value("COMPLETE")
  val NEW = Value("NEW")
  val SKIP = Value("SKIP")


}
