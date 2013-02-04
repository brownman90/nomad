package com.nevilon.nomad.crawler

import org.apache.commons.lang.builder.{HashCodeBuilder, EqualsBuilder}
import com.nevilon.nomad.filter.Action

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/11/13
 * Time: 3:18 PM
 */


/*
class RawUrlRelation(val from: String, val title: String, val to: String, val action: Action.Action) {

  override def equals(obj: Any): Boolean = {
    if (obj.isInstanceOf[RawUrlRelation]) {
      val other = obj.asInstanceOf[RawUrlRelation]
      new EqualsBuilder()
        .append(from, other.from)
        .append(to, other.to) //add Action?
        .isEquals()
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    new HashCodeBuilder()
      .append(from)
      .append(to) //add action?
      .toHashCode()
  }

}
*/


class UrlStatus {}


object UrlStatus extends Enumeration {

  val InProgress = Value("IN_PROGRESS")
  val Skip = Value("SKIP")
  val Complete = Value("COMPLETE")
  val New = Value("NEW")


}

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


class Url(val location: String, val status: UrlStatus.Value, val id: String, val fileId: String, val title: String, val action: Action.Value) {

  def updateStatus(newStatus: UrlStatus.Value): Url = {
    new Url(location, newStatus, id, fileId, title, action)
  }

  def updateFileId(newFileId: String): Url = {
    new Url(location, status, id, newFileId, title, action)
  }


  override def equals(obj: Any): Boolean = {
    if (obj.isInstanceOf[Url]) {
      val other = obj.asInstanceOf[Url]
      //add fileId?
      new EqualsBuilder()
        .append(location, other.location)
        .isEquals()
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    new HashCodeBuilder()
      .append(location)
      .toHashCode()
  }
}


