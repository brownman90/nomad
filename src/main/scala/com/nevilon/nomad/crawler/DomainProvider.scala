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

import com.nevilon.nomad.storage.graph.SynchronizedDBService


class DomainInjector(dbService: SynchronizedDBService) {

  def inject(url: String) {
    val normalizedUrl = URLUtils.normalize(url)
    val domainStr = URLUtils.getDomainName(normalizedUrl)
    dbService.createDomainIfNeeded(new Domain(domainStr, DomainStatus.NEW))
    dbService.getUrl(normalizedUrl) match {
      case None => {
        val url = dbService.saveOrUpdateUrl(new Url(normalizedUrl, UrlStatus.NEW))
        dbService.addUrlToDomain(url)
      }
      case Some(v) => //should exists
    }


  }

}




