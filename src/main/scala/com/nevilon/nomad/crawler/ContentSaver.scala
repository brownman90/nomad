package com.nevilon.nomad.crawler

import java.io.InputStream
import com.nevilon.nomad.storage.graph.FileStorage

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/10/13
 * Time: 9:53 AM
 */
class ContentSaver(fileStorage: FileStorage) {


  def saveContent(is: InputStream, url: String, contentType: String): String = {
    fileStorage.saveStream(is, url, contentType) match {
      case Some(fileId) => fileId
      case None => {
        throw new RuntimeException("Unable to save file")
      }
    }
  }

}
