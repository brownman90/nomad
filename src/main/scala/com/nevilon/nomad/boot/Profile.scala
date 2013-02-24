package com.nevilon.nomad.boot

import java.io.File
import com.nevilon.nomad.logs.Logs

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/24/13
 * Time: 2:18 PM
 */
class Profile(pathToFolder: String) extends Logs {

  private val seedFileName = "seed.txt"
  private val filterFileName = "filters.groovy"
  private val appConfFileName = "application.conf"

  if (!new File(pathToFolder).exists()) {
    error("profile dir not found")
    throw new Error("profile dir not found")
  }


  val seedFile = buildFile(seedFileName)
  val filterFile = buildFile(filterFileName)
  val appConfFile = buildFile(appConfFileName)


  private def buildFile(fileName: String): File = {
    val file = new File(pathToFolder + "/" + fileName)
    if (!file.exists()) {
      error("cannon found some config file")
      throw new Error("cannot found some config file")
    }
    file
  }


}
