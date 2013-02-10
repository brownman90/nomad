package com.nevilon.nomad.crawler

import org.apache.http.client.HttpClient
import com.nevilon.nomad.storage.graph.{FileStorage, TitanDBService}
import com.nevilon.nomad.filter.{Action, FilterProcessorFactory}
import org.apache.http.HttpEntity
import java.io.{ByteArrayInputStream, InputStream}
import org.apache.http.util.EntityUtils
import scala.util.{Try, Success}
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

  private val carousel = new Carousel[ExtractedData](maxThreads, linkProvider)
  carousel.setOnStart((url: Url) => loadAndProcess(url))
  carousel.setOnComplete(onProcessingComplete)
  carousel.setOnBeforeStart((url: Url) => (dbService.addOrUpdateUrl(url.updateStatus(UrlStatus.InProgress))))


  def stop() {}

  def begin() {
    linkProvider.findOrCreateUrl(startUrl)
    carousel.start()
  }

  private var count = 0


  private def saveHttpEntity(entityParams: EntityParams, entity: HttpEntity, url: Url): Option[FetchedContent] = {
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
          case None => new FetchedContent(gfsId, entityParams, null)
          case Some(content) => new FetchedContent(gfsId, entityParams, content)
        }
      }
      Some(fetchedContent)
    } else {
      info("skip " + url)
      //???
      None
    }
  }

  private def loadAndProcess(url: Url): ExtractedData = {
    count += 1
    info("total crawled: " + count)

    val fetcher = new Fetcher(url, httpClient)
    fetcher.onException((e: Exception) => {
      info("error during crawling " + url, e)
    })
    fetcher.load(saveHttpEntity) match {
      case None => {
        throw new RuntimeException("shit!")
      }
      case Some(value) => {
        value.content match {
          case null => {
            // this is a case for non html data, but we still need process fileId!
            new ExtractedData(Nil, value)
          }
          case content => {
            processHtmlContent(value, url)
          }
        }
      }
    }
  }


  private def processHtmlContent(value: FetchedContent, url: Url): ExtractedData = {
    val page = pageDataExtractor.extractLinks(value.content, url.location)
    info("links extracted: " + page.links.length + " from " + url.location)
    //build urlrelations objects
    val relations = page.links.map(item => {
      new Relation(url, new Url(item.url))
    })
    //remove invalid links
    val clearedLinks = URLUtils.clearUrlRelations(startUrl, relations.toList)
    //pass to filter
    val filteredRawUrlRelations = clearedLinks.map(relation => {
      val action = filterProcessor.filterUrl(relation.to.location)
      val toUrl = relation.to.updateAction(action)
      new Relation(relation.from, toUrl)
    })
    //remove all links we needn't to crawl
    val linksToProcess = filteredRawUrlRelations.filter(relation => {
      if (relation.to.action == Action.Download) {
        // refactor this!
        true
      } else {
        info("skipped url " + relation.to.location)
        false
      }
    })
    new ExtractedData(linksToProcess, value)
  }

  private def onProcessingComplete(tryExtractedData: Try[ExtractedData], url: Url) {
    tryExtractedData match {
      case Success(extractedData) => {
        // and what about fail? do we need to change status?
        synchronized {
          //ERROR and SKIP status also!!!
          dbService.addOrUpdateUrl(
            url.
              updateStatus(UrlStatus.Complete).
              updateFileId(extractedData.fetchedContent.gfsId)
          )
          extractedData.relations match {
            case Nil => info("some non text/html file have been downloaded from " + url.location)
            case _ => extractedData.relations.foreach(linkProvider.addToExtractedLinks(_))
          }
          carousel.start()
        }

      }
      case _ => error("some kind of shit during crawling " + url.location)
    }
  }


}


