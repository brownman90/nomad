package com.nevilon.nomad.utils

import java.lang.reflect.Field

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/24/13
 * Time: 10:38 AM
 */
trait StringGenerator {

  override def toString() = {
    getClass().getDeclaredFields().map {
      field: Field =>
        field.setAccessible(true)
        field.getName() + " : " + field.get(this).toString()
    }.deep.mkString("; ")
  }

}
