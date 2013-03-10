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

import com.thinkaurelius.titan.core.{TitanTransaction, TitanGraph}
import com.nevilon.nomad.crawler.{Transformers, Url}
import com.tinkerpop.blueprints.Vertex


class UrlService(implicit graph: TitanGraph) extends TransactionSupport {


  def saveOrUpdateUrl(url: Url): Url = {
    withTransaction {
      implicit tx => {
        Transformers.vertex2Url(saveOrUpdateUrlInTx(url))
      }
    }
  }


  def getUrl(location: String): Option[Url] = {
    withTransaction[Option[Url]] {
      implicit tx =>
        getUrlInTx(location) match {
          case None => None
          case Some(v) => Some(Transformers.vertex2Url(v))
        }
    }
  }

  def saveOrUpdateUrlInTx(url: Url)(implicit tx: TitanTransaction): Vertex = {
    val vertex = {
      getUrlInTx(url.location) match {
        case None => {
          val newV = tx.addVertex()
          newV.setProperty(GraphProperties.Url.locationProperty, url.location)
          newV
        }
        case Some(v) => v
      }
    }
    vertex.setProperty(GraphProperties.Url.statusProperty, url.status.toString)
    vertex.setProperty(GraphProperties.Url.fileIdProperty, url.fileId)
    vertex
  }

  def getUrlInTx(location: String)(implicit tx: TitanTransaction): Option[Vertex] = {
    val vertex = tx.getVertex(GraphProperties.Url.locationProperty, location)
    if (vertex == null) None else Some(vertex)
  }


}