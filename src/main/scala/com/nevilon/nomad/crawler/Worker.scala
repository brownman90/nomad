package com.nevilon.nomad.crawler

import org.apache.http.client.HttpClient
import com.nevilon.nomad.storage.graph.{FileStorage, TitanDBService}
import com.nevilon.nomad.filter.{Action, FilterProcessorFactory}
import org.apache.http.HttpEntity
import java.io.{ByteArrayInputStream, InputStream}
import org.apache.http.util.EntityUtils
import util.{Failure, Try, Success}
import com.nevilon.nomad.logs.Logs

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/1/13
 * Time: 10:20 AM
 */

class Worker(startUrl: String, val maxThreads: Int, httpClient: HttpClient, dbService: TitanDBService) extends Logs {

  private val fileStorage = new FileStorage()

  private val contentSaver = new ContentSaver(fileStorage)
  private val linkProvider = new LinkProvider(startUrl, dbService)
  private val pageDataExtractor = new PageDataExtractor

  private val domain = URLUtils.normalize(URLUtils.getDomainName(startUrl))
  private val filterProcessor = FilterProcessorFactory.get(domain)

  private val carousel = new Carousel(maxThreads, linkProvider)
  carousel.setOnStart((url: Url) => loadAndProcess(url))
  carousel.setOnBeforeStart((url: Url) => (dbService.addOrUpdateUrl(url.updateStatus(UrlStatus.IN_PROGRESS))))


  def stop() {}

  def begin() {
    linkProvider.findOrCreateUrl(startUrl)
    carousel.start()
  }

  private var count = 0


  private def loadAndProcess(url: Url) {
    count += 1
    info("total crawled: " + count)

    val fetcher = new Fetcher(url, httpClient)
    fetcher.onException((e: Exception) => {
      dbService.addOrUpdateUrl(url.updateStatus(UrlStatus.ERROR))
      info("error during crawling " + url, e)
    })
    fetcher.onHttpError((code: Int) => {
      dbService.addOrUpdateUrl(url.updateStatus(UrlStatus.HTTP_ERROR))
      info("http error during crawling " + url.location + " error " + code)
    })

    fetcher.onFinish(() => {
      carousel.start()
    })

    fetcher.onDataStream((entityParams: EntityParams, entity: HttpEntity, url: Url) => {
      if (filterProcessor.filterEntity(entityParams) == Action.Download) {

        val data: (InputStream, Option[String]) = {
          if (entityParams.mimeType.getSubType.contains("html")) {
            val contentAsTxt = EntityUtils.toString(entity)
            (new ByteArrayInputStream(contentAsTxt.getBytes), Some(contentAsTxt))
          } else {
            (entity.getContent, None)
          }
        }

        val gfsId = contentSaver.saveContent(data._1, url.location, entityParams.mimeType.getBaseType)
        val fetchedContent = {
          data._2 match {
            case None => {
              //binary file
              new ExtractedData(Nil, new FetchedContent(gfsId, entityParams, null))
            }
            case Some(content) => {
              //html file
              processHtmlContent(new FetchedContent(gfsId, entityParams, content), url)
            }
          }
        }
        onProcessingComplete(fetchedContent, url)
      } else {
        dbService.addOrUpdateUrl(url.updateStatus(UrlStatus.SKIP))
        info("skipped entity " + url.location)
      }
    })
    fetcher.load()
  }


  private def processHtmlContent(value: FetchedContent, url: Url): ExtractedData = {
    val page = pageDataExtractor.extractLinks(value.content, url.location)
    info("links extracted: " + page.links.length + " from " + url.location)
    //build urlrelations objects
    val relations = page.links.map(item => {
      new Relation(url, new Url(item.url, UrlStatus.NEW))
    })
    //remove invalid links
    val clearedLinks = URLUtils.clearUrlRelations(startUrl, relations.toList)
    //pass to filter
    val filteredRawUrlRelations = clearedLinks.map(relation => {
      val action = filterProcessor.filterUrl(relation.to.location)

      val status = {
        action match {
          case Action.Download => UrlStatus.NEW
          case Action.Skip => {
            info("skipped url " + relation.to.location)
            UrlStatus.SKIP
          }
        }
      }

      val toUrl = relation.to.updateStatus(status)
      new Relation(relation.from, toUrl)
    })

    new ExtractedData(filteredRawUrlRelations, value)
  }

  private def onProcessingComplete(extractedData: ExtractedData, url: Url) {
    synchronized {
      dbService.addOrUpdateUrl(
        url.
          updateStatus(UrlStatus.COMPLETE).
          updateFileId(extractedData.fetchedContent.gfsId)
      )
      extractedData.relations.foreach(linkProvider.addToExtractedLinks(_))
    }
  }


}