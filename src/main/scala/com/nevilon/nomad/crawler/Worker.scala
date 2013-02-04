package com.nevilon.nomad.crawler

import org.apache.http.client.HttpClient
import com.nevilon.nomad.storage.graph.{FileStorage, TitanDBService}
import org.apache.log4j.LogManager
import collection.mutable.ListBuffer
import concurrent._
import com.nevilon.nomad.filter.{FilterProcessor, Action, FilterProcessorFactory}
import org.apache.http.{HttpResponse, HttpEntity}
import javax.activation.MimeType
import java.io.{ByteArrayInputStream, InputStream}
import org.apache.commons.httpclient.util.URIUtil
import org.apache.http.client.methods.HttpGet
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.util.EntityUtils
import scala.util.Success

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/1/13
 * Time: 10:20 AM
 */
//use startUrl, not domain!!!
class Worker(startUrl: String, val maxThreads: Int, httpClient: HttpClient, dbService: TitanDBService) {

  private val fileStorage = new FileStorage()

  private val logger = LogManager.getLogger(this.getClass.getName)

  private val linkProvider = new LinkProvider(startUrl, dbService)
  private val pageDataExtractor = new PageDataExtractor
  private var futures = new ListBuffer[Future[ExtractedData]]

  private val domain = URLUtils.normalize(URLUtils.getDomainName(startUrl))
  private val filterProcessor = FilterProcessorFactory.get(domain)

  def stop() {}

  def begin() {
    linkProvider.findOrCreateUrl(startUrl)
    initCrawling()
  }


  private var count = 0


  private def buildEntityParams(httpEntity: HttpEntity, url: String): EntityParams = {
    val mimeType = new MimeType(httpEntity.getContentType.getValue)
    val entityParams = new EntityParams(httpEntity.getContentLength, url, mimeType)
    entityParams
  }


  private def saveContent(is: InputStream, url: String, contentType: String): String = {
    fileStorage.saveStream(is, url, contentType) match {
      case Some(fileId) => fileId
      case None => {
        throw new RuntimeException("Unable to save file")
      }
    }
  }

  private def fetch(url: String): Option[FetchedContent] = {
    //encode to escape special symbols
    val encodedUrl = URIUtil.encodeQuery(url)
    val httpGet = new HttpGet(encodedUrl)
    logger.info("connecting to " + encodedUrl)
    try {
      val response: HttpResponse = httpClient.execute(httpGet, new BasicHttpContext()) //what is context?
      val entity: HttpEntity = response.getEntity
      val entityParams = buildEntityParams(entity, url)
      if (filterProcessor.filterEntity(entityParams) == Action.Download) {
        val data: (InputStream, Option[String]) = {
          if (entityParams.mimeType.getSubType.contains("html")) {
            val contentAsTxt = EntityUtils.toString(entity)
            (new ByteArrayInputStream(contentAsTxt.getBytes), Some(contentAsTxt))
          } else {
            (entity.getContent, None)
          }
        }
        val gfsId = saveContent(data._1, url, entityParams.mimeType.getBaseType)
        val fetchedContent = {
          data._2 match {
            case None => new FetchedContent(gfsId, entityParams, null)
            case Some(content) => new FetchedContent(gfsId, entityParams, content)
          }
        }
        Some(fetchedContent)
      } else {
        logger.info("skip " + url)
        //???
        None
      }
    }
    catch {
      case e: Exception => {
        logger.info("error during crawling " + url, e)
        httpGet.abort()
        None
      }
    }
    finally {
      httpGet.abort()
    }
  }

  private def crawlUrl(url: Url, filterProcessor: FilterProcessor): Future[ExtractedData] = {
    implicit val ec = ExecutionContext.Implicits.global
    val location = url.location
    val thisFuture = future[ExtractedData] {
      val fetchedContent = fetch(location)
      count += 1
      logger.info("total crawled: " + count)

      fetchedContent match {
        case None => {
          throw new RuntimeException("shit!")
        } //exception
        case Some(value) => {
          value.content match {
            case null => {
              // this is a case for non html data, but we still need process fileId!
              new ExtractedData(Nil, value)
            }
            case content => {
              val page = pageDataExtractor.extractLinks(value.content, location)
              logger.info("links extracted: " + page.links.length + " from " + location)
              //build urlrelations objects
              val relations = page.links.map(item => {
                val to = new Url(item.url, item.text)
                new Relation(url, to)
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
                  logger.info("skipped url " + relation.to.location)
                  false
                }
              })
              new ExtractedData(linksToProcess, value)
            }
          }
        }
      }
    }

    thisFuture onComplete {
      // in what thread does this execs?
      case Success(extractedData) => {
        // and what about fail? do we need to change status?
        synchronized {
          //ERROR and SKIP status also!!!
          dbService.updateUrl(
            url.updateStatus(UrlStatus.Complete).
              updateFileId(extractedData.fetchedContent.gfsId)
          )
          futures -= thisFuture
          if (extractedData.relations == Nil) {
            // refactor this - remove nulls and null for collections!
            logger.info("some non text/html file have been downloaded from " + location)
          } else {
            extractedData.relations.foreach(linkProvider.addToExtractedLinks(_))
          }
          initCrawling()
        }
      }
      case _ => logger.error("some king of shit during crawling " + location)
    }
    thisFuture
  }

  private def initCrawling() {
    var hasUrlsToCrawl = true
    while (futures.length < maxThreads && hasUrlsToCrawl) {
      linkProvider.urlToCrawl() match {
        case None => {
          hasUrlsToCrawl = false // exit from loop
          logger.info("sorry, no links to crawl")
        }
        case Some(url) => {
          dbService.updateUrl(url.updateStatus(UrlStatus.InProgress))
          val newF = crawlUrl(url, filterProcessor)
          futures += newF
          logger.info("starting future for crawling " + url.location)
        }
      }
    }
  }

}


//refactor
class FetchedContent(val gfsId: String, val entityParams: EntityParams, val content: String)

//refactor
class ExtractedData(val relations: List[Relation], val fetchedContent: FetchedContent)

//refactor
class EntityParams(val size: Long, val url: String, val mimeType: MimeType)

