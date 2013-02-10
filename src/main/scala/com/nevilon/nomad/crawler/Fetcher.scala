package com.nevilon.nomad.crawler

import org.apache.http.client.HttpClient
import org.apache.http.{HttpResponse, HttpEntity}
import org.apache.commons.httpclient.util.URIUtil
import org.apache.http.client.methods.HttpGet
import org.apache.http.protocol.BasicHttpContext
import com.nevilon.nomad.logs.Logs
import javax.activation.MimeType

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/10/13
 * Time: 9:38 AM
 */
class Fetcher(url: Url, httpClient: HttpClient) extends Logs {

  private var onExceptionHandler: (Exception) => Unit = null

  private def buildEntityParams(httpEntity: HttpEntity, url: String): EntityParams = {
    val mimeType = new MimeType(httpEntity.getContentType.getValue)
    val entityParams = new EntityParams(httpEntity.getContentLength, url, mimeType)
    entityParams
  }


  def load(method: (EntityParams, HttpEntity, Url) => Option[FetchedContent]): Option[FetchedContent] = {
    val encodedUrl = URIUtil.encodeQuery(url.location)
    val httpGet = new HttpGet(encodedUrl)
    info("connecting to " + encodedUrl)
    try {
      val response: HttpResponse = httpClient.execute(httpGet, new BasicHttpContext()) //what is context?
      val entity: HttpEntity = response.getEntity
      val entityParams = buildEntityParams(entity, url.location)
      method(entityParams, entity, url)
    } catch {
      case e: Exception => {
        info("error during crawling " + url, e)
        httpGet.abort()
        onExceptionHandler
        None
      }
    }
    finally {
      httpGet.abort()
    }
  }

  def onException(handler: (Exception) => Unit) {
    onExceptionHandler = handler
  }

}
