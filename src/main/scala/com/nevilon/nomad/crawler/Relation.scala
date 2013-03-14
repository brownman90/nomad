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

import org.apache.commons.lang.builder.{HashCodeBuilder, EqualsBuilder}


class Relation(val from: Url, val to: Url) {

  override def equals(obj: Any): Boolean = {
    if (obj.isInstanceOf[Relation]) {
      val other = obj.asInstanceOf[Relation]
      new EqualsBuilder()
        .append(from.location, other.from.location)
        .append(to.location, other.to.location) //add Action?
        .isEquals()
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    new HashCodeBuilder()
      .append(from.location)
      .append(to.location) //add action?
      .toHashCode()
  }

}
