package com.nevilon.nomad.crawler

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.PoolingClientConnectionManager
import org.apache.http.conn.ConnectionKeepAliveStrategy
import org.apache.http.{HeaderElement, HeaderElementIterator, HttpResponse}
import org.apache.http.protocol.{HTTP, HttpContext}
import org.apache.http.message.BasicHeaderElementIterator
import org.apache.http.conn.scheme.{PlainSocketFactory, Scheme, SchemeRegistry}
import org.apache.http.conn.ssl.{TrustStrategy, SSLSocketFactory}
import java.security.cert

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/11/13
 * Time: 11:51 AM
 */
object HttpClientFactory {


  private val stubSSLSocketFactory = new SSLSocketFactory(new TrustStrategy() {

    def isTrusted(chain: Array[cert.X509Certificate], authType: String): Boolean = true
  }, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)


  def buildHttpClient(threadsTotal: Int, threadsPerHost: Int): DefaultHttpClient = {
    //set custom https factory to accept all https certs (self signed and incorrect)
    val schemeRegistry = new SchemeRegistry()
    schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory))
    schemeRegistry.register(new Scheme("https", 443, stubSSLSocketFactory))
    //set threading params
    val cm: PoolingClientConnectionManager = new PoolingClientConnectionManager(schemeRegistry)
    cm.setMaxTotal(threadsTotal)
    cm.setDefaultMaxPerRoute(threadsPerHost)
    //build client
    val httpClient: DefaultHttpClient = new DefaultHttpClient(cm)
    //active usage of keep-alive
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


