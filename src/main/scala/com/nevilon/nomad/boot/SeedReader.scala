package com.nevilon.nomad.boot

import java.io.File
import io.Source

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 2/14/13
 * Time: 6:39 AM
 */
class SeedReader(file: File) {

  private val seeds: List[String] = Source.fromFile(file).getLines().toList

  def getSeeds: List[String] = seeds


}
