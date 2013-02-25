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

import java.io.InputStream
import com.nevilon.nomad.storage.graph.FileStorage


class ContentSaver(fileStorage: FileStorage) {


  def saveContent(is: InputStream, url: String, contentType: String, urlId:String): String = {
    fileStorage.saveStream(is, url, contentType, urlId) match {
      case Some(fileId) => fileId
      case None => {
        throw new RuntimeException("Unable to save file")
      }
    }
  }

}
