package com.nevilon.nomad.crawler

import javax.activation.MimeType

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/10/13
 * Time: 10:01 AM
 */


class FetchedContent(val gfsId: String, val entityParams: EntityParams, val content: String)

class ExtractedData(val relations: List[Relation], val fetchedContent: FetchedContent)

class EntityParams(val size: Long, val url: String, val mimeType: MimeType)
