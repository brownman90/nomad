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

import javax.activation.MimeType


class FetchedContent(val gfsId: String, val entityParams: EntityParams, val content: String)

class ExtractedData(val relations: List[Relation], val fetchedContent: FetchedContent)

class EntityParams(val size: Long, val url: String, val mimeType: MimeType)
