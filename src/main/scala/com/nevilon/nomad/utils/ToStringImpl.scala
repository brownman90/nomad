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
package com.nevilon.nomad.utils

import java.lang.reflect.Field

trait ToStringImpl {

  override def toString() = {
    getClass().getDeclaredFields().map {
      field: Field =>
        field.setAccessible(true)
        field.getName() + " : " + field.get(this).toString()
    }.deep.mkString("; ")
  }

}
