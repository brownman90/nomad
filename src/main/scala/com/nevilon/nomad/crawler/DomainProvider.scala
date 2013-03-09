package com.nevilon.nomad.crawler

import com.nevilon.nomad.storage.graph.{SynchronizedDBService, DomainService}
import org.apache.commons.lang.builder.{HashCodeBuilder, EqualsBuilder}

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 3/8/13
 * Time: 12:04 PM
 */
class DomainProvider(dbService: SynchronizedDBService) {

  def add(domain: Domain) {
    dbService.createDomainIfNeeded(domain)
  }

  def get() {
    dbService.getDomainWithStatus(DomainStatus.NEW)
  }

  def updateDomain(domain: Domain) {
    dbService.updateDomain(domain)
  }

}

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