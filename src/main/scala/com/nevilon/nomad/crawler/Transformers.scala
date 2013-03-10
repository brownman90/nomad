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

import com.tinkerpop.blueprints.Element
import com.nevilon.nomad.storage.graph.GraphProperties


object Transformers {

  private implicit def AnyRef2String(property: AnyRef) = property.toString


  implicit def vertex2Url(element: Element): Url = {
    //get status value
    val statusProperty = element.getProperty(GraphProperties.Url.statusProperty)
    val status = UrlStatus.withName(statusProperty)
    //get action value
    //get str value of property by calling toString
    new Url(element.getProperty(GraphProperties.Url.locationProperty), status, element.getId,
      element.getProperty(GraphProperties.Url.fileIdProperty))
  }

  implicit def vertex2Domain(element: Element): Domain = {
    val status = DomainStatus.withName(element.getProperty(GraphProperties.Domain.statusProperty))
    new Domain(element.getProperty(GraphProperties.Domain.nameProperty), status)
  }

}
