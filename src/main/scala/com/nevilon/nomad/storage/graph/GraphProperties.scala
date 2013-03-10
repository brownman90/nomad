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
package com.nevilon.nomad.storage.graph


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
