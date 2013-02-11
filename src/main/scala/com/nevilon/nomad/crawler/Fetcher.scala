package com.nevilon.nomad.crawler

import org.apache.http.client.HttpClient
import org.apache.http.{HttpStatus, HttpResponse, HttpEntity}
import org.apache.commons.httpclient.util.URIUtil
import org.apache.http.client.methods.HttpGet
import org.apache.http.protocol.BasicHttpContext
import com.nevilon.nomad.logs.Logs
import javax.activation.MimeType
import scala.Unit

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/10/13
 * Time: 9:38 AM
 */
class Fetcher(url: Url, httpClient: HttpClient) extends Logs {

  private var onExceptionHandler: (Exception) => Unit = null
  private var onHttpErrorHandler: (Int) => Unit = null
  private var onDataStreamHandler: (EntityParams, HttpEntity, Url) => Unit = null
  private var onFinishHandler: () => Unit = null


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
        onDataStreamHandler(entityParams, entity, url)
      } else {
        onHttpErrorHandler(statusCode)
      }
    } catch {
      case e: Exception => {
        httpGet.abort()
        onExceptionHandler(e)
      }
    }
    finally {
      httpGet.abort()
      onFinishHandler()
    }
  }

  def onDataStream(handler: (EntityParams, HttpEntity, Url) => Unit) {
    onDataStreamHandler = handler

  }

  def onHttpError(handler: (Int) => Unit) {
    onHttpErrorHandler = handler
  }

  def onException(handler: (Exception) => Unit) {
    onExceptionHandler = handler
  }

  def onFinish(handler: () => Unit) {
    onFinishHandler = handler
  }

}
