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

class Url(
           val location: String, val status: UrlStatus.Value,
           val id: String, val fileId: String
           ) {

  def this(location: String, status: UrlStatus.Value) = this(location, UrlStatus.NEW, "none", "none")


  def updateLocation(newLocation: String) = new Url(newLocation, status, id, fileId)

  def updateStatus(newStatus: UrlStatus.Value) = new Url(location, newStatus, id, fileId)

  def updateFileId(newFileId: String) = new Url(location, status, id, newFileId)


  override def equals(obj: Any): Boolean = {
    if (obj.isInstanceOf[Url]) {
      val other = obj.asInstanceOf[Url]
      //add fileId?
      new EqualsBuilder()
        .append(location, other.location)
        .isEquals
    } else false
  }

  override def hashCode() = {
    new HashCodeBuilder()
      .append(location)
      .toHashCode
  }
}

class UrlStatus {}


object UrlStatus extends Enumeration {

  val IN_PROGRESS = Value("IN_PROGRESS")
  val SKIP = Value("SKIP")
  val COMPLETE = Value("COMPLETE")
  val NEW = Value("NEW")
  val ERROR = Value("ERROR")
  val HTTP_ERROR = Value("HTTP_ERROR")
  //val DROP  = Value("DROP")


}
