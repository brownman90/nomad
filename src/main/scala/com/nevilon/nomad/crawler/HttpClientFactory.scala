package com.nevilon.nomad.crawler

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.conn.ConnectionKeepAliveStrategy
import org.apache.http.{HeaderElement, HeaderElementIterator, HttpResponse}
import org.apache.http.protocol.{HTTP, HttpContext}
import org.apache.http.message.BasicHeaderElementIterator

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/11/13
 * Time: 11:51 AM
 */
object HttpClientFactory {


  def buildHttpClient(threadsTotal: Int, threadsPerHost: Int): DefaultHttpClient = {
    val cm: PoolingClientConnectionManager = new PoolingClientConnectionManager
    cm.setMaxTotal(threadsTotal)
    cm.setDefaultMaxPerRoute(threadsPerHost)

    val httpClient: DefaultHttpClient = new DefaultHttpClient(cm)

    httpClient.setKeepAliveStrategy(new ConnectionKeepAliveStrategy {
      def getKeepAliveDuration(response: HttpResponse, context: HttpContext): Long = {
        val it: HeaderElementIterator = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE))
        while (it.hasNext) {
          val he: HeaderElement = it.nextElement
          val param: String = he.getName
          val value: String = he.getValue
          if (value != null && param.equalsIgnoreCase("timeout")) {
            try {
              return value.toLong * 1000
            }
            catch {
              case ignore: NumberFormatException => {
                ignore.printStackTrace()
              }
            }
          }
        }
        30 * 1000 // ???

      }
    })
    httpClient
  }

}
