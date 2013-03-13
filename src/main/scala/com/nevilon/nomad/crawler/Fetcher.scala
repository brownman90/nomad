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

import org.apache.http.client.HttpClient
import org.apache.http.{HttpStatus, HttpResponse, HttpEntity}
import org.apache.commons.httpclient.util.URIUtil
import org.apache.http.client.methods.HttpGet
import org.apache.http.protocol.BasicHttpContext
import com.nevilon.nomad.logs.Logs
import javax.activation.MimeType
import scala.Unit


abstract class Fetcher(url: Url, httpClient: HttpClient) extends Logs {

  def onException(ex: Exception): Unit

  def onHttpError(httError: Int): Unit

  def onDataStream(entityParams: EntityParams, httpEntity: HttpEntity, url: Url): Unit


  private def buildEntityParams(httpEntity: HttpEntity, url: String): EntityParams = {
    val mimeType = new MimeType(httpEntity.getContentType.getValue)
    val entityParams = new EntityParams(httpEntity.getContentLength, url, mimeType)
    entityParams
  }


  def load() {
    val encodedUrl = URIUtil.encodeQuery(url.location)
    val httpGet = new HttpGet(encodedUrl)
    info("connecting to " + encodedUrl)
    try {
      val response: HttpResponse = httpClient.execute(httpGet, new BasicHttpContext()) //what is context?
      val statusCode = response.getStatusLine.getStatusCode
      if (statusCode == HttpStatus.SC_OK) {
        val entity: HttpEntity = response.getEntity
        val entityParams = buildEntityParams(entity, url.location)
        onDataStream(entityParams, entity, url)
      } else {
        onHttpError(statusCode)
      }
    } catch {
      case e: Exception => {
        httpGet.abort()
        onException(e)
      }
    }
    finally {
      httpGet.abort()
    }
  }
}
