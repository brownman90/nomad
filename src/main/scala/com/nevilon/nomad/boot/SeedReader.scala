package com.nevilon.nomad.boot

import java.io.File
import io.Source

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/14/13
 * Time: 6:39 AM
 */
class SeedReader(pathToFile: String) {

  private val seeds: List[String] = Source.fromFile(new File(pathToFile)).getLines().toList

  def getSeeds:List[String]= seeds


}
