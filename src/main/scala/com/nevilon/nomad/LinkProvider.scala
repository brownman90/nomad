package com.nevilon.nomad

import collection.mutable.ListBuffer
//import com.nevilon.nomad.Types.LinkRelation

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/11/13
 * Time: 2:18 PM
 */
class LinkProvider(val domain:String) {
   /*

  private val dbService = new DBService
  //dbService.addDomain(domain)

  val unvisited = ListBuffer[String]()
  // add all but start domain!
  val blacklist = new ListBuffer[String]()
  // extension?
  val visited = new ListBuffer[String]() // visited links?

  def addPage(parentPageUrl: String, childPagedUrl: String){
    //println(parentPageUrl+" # " + childPagedUrl)
    dbService.linkUrls(URLUtils.normalize(parentPageUrl), URLUtils.normalize(childPagedUrl))
  }


  def addNewLinks(result: LinkRelation) {
    var cleardLinks = result._2.filter(newLink => (!unvisited.contains(newLink) && !visited.contains(newLink)))
    //remove email links
    cleardLinks = cleardLinks.filter(newLink => {
      !newLink.contains("@")
    })
    //remove empty links
    cleardLinks = cleardLinks.filter(newLink => {
      !newLink.trim().isEmpty
    })
    //remove links to another domains
    cleardLinks = cleardLinks.filter(newLink => {
      try {
        val startDomain = URLUtils.getDomainName(domain)
        val linkDomain = URLUtils.getDomainName(newLink)
        startDomain.equals(linkDomain)
      }
      catch {
        case e: Exception => {
          println(e)
        }
        false
      }
    })

    cleardLinks.foreach(url => {
      addPage(result._1, url)
    })
    unvisited ++= cleardLinks
    // println("duplicates: " + (result._2.length - cleardLinks.length))
    println("visited: " + visited.length + " " + result._1 + " unvisited: " + unvisited.length)
  }
  */

}
