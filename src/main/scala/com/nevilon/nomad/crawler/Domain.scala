package com.nevilon.nomad.crawler

import org.apache.commons.lang.builder.{EqualsBuilder, HashCodeBuilder}

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 3/10/13
 * Time: 9:50 AM
 */
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
