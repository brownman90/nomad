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

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.conn.ConnectionKeepAliveStrategy
import org.apache.http.HttpResponse
import org.apache.http.protocol.{HTTP, HttpContext}
import org.apache.http.message.BasicHeaderElementIterator
import org.apache.http.conn.scheme.{PlainSocketFactory, Scheme, SchemeRegistry}
import org.apache.http.conn.ssl.{TrustStrategy, SSLSocketFactory}
import java.security.cert
import org.apache.http.params.CoreProtocolPNames


object HttpClientFactory {


  private val stubSSLSocketFactory = new SSLSocketFactory(new TrustStrategy() {

    def isTrusted(chain: Array[cert.X509Certificate], authType: String) = true
  }, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)


  def buildHttpClient(threadsTotal: Int, threadsPerHost: Int, userAgent: String): DefaultHttpClient = {
    val cm: PoolingClientConnectionManager = new PoolingClientConnectionManager(buildSchemeRegistry())
    cm.setMaxTotal(threadsTotal)
    cm.setDefaultMaxPerRoute(threadsPerHost)
    val httpClient = new DefaultHttpClient(cm)
    httpClient.getParams().setParameter(CoreProtocolPNames.USER_AGENT, userAgent)
    httpClient.setKeepAliveStrategy(buildKeepAliveStrategy())
    httpClient
  }

  private def buildSchemeRegistry(): SchemeRegistry = {
    val schemeRegistry = new SchemeRegistry()
    schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory))
    schemeRegistry.register(new Scheme("https", 443, stubSSLSocketFactory))
    schemeRegistry
  }

  private def buildKeepAliveStrategy(): ConnectionKeepAliveStrategy = {
    new ConnectionKeepAliveStrategy {
      def getKeepAliveDuration(response: HttpResponse, context: HttpContext): Long = {
        val it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE))
        while (it.hasNext) {
          val he = it.nextElement
          val param = he.getName
          val value = he.getValue
          if (value != null && param.equalsIgnoreCase("timeout")) {
            try
              return value.toLong * 1000
            catch {
              case ignore: NumberFormatException => {
                ignore.printStackTrace()
              }
            }
          }
        }
        30 * 1000
      }
    }
  }

}


