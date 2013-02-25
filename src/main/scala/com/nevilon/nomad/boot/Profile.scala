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
package com.nevilon.nomad.boot

import java.io.File
import com.nevilon.nomad.logs.Logs


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
