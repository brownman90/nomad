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

import com.tinkerpop.blueprints.{Element, Vertex}


object Transformers {

  private implicit def AnyRef2String(property: AnyRef) = property.toString


  implicit def vertex2Url(element: Element): Url = {
    //get status value
    val statusProperty = element.getProperty("status")
    val status = UrlStatus.withName(statusProperty)
    //get action value
    //get str value of property by calling toString
    new Url(element.getProperty("location"), status, element.getId,
      element.getProperty("fileId"))
  }

  implicit def vertex2Domain(element: Element): Domain = {
    val status = DomainStatus.withName(element.getProperty("status"))
    new Domain(element.getProperty("domain"), status)
  }

}
