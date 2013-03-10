package com.nevilon.nomad.storage.graph

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 3/5/13
 * Time: 2:43 PM
 */

object GraphProperties {

  object Domain {
    val statusProperty = "domain_status"
    val nameProperty = "domain_name"
    val urlEdgeLabel = "domain_url_link"
  }

  object Url {
    val locationProperty = "url_location"
    val statusProperty = "url_status"
    val fileIdProperty = "url_fileId"
  }

}
