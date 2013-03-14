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
