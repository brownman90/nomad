package com.nevilon.nomad

import org.apache.commons.lang.builder.{HashCodeBuilder, EqualsBuilder}

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/11/13
 * Time: 3:18 PM
 */


class RawLinkRelation(val from: String, val to: String) {

  override def equals(obj: Any): Boolean = {
    if (obj.isInstanceOf[RawLinkRelation]) {
      val other = obj.asInstanceOf[RawLinkRelation]
      new EqualsBuilder()
        .append(from, other.from)
        .append(to, other.to)
        .isEquals()
    } else {
      false
    }
  }

  override def hashCode(): Int = {
    new HashCodeBuilder()
      .append(from)
      .append(to)
      .toHashCode()
  }

}


class UrlStatus {}


object UrlStatus extends Enumeration {

  val InProgress = Value("IN_PROGRESS")
  val Skip = Value("SKIP")
  val Complete = Value("COMPLETE")
  val New = Value("NEW")


}


