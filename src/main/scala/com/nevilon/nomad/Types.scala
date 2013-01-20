package com.nevilon.nomad

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/11/13
 * Time: 3:18 PM
 */
object Types {

  //type LinkRelation = (String, List[String])
  //parent - child
  type LinksTree = (String, List[String])
  //type LinkRelation = (String, String)

}

class RawLinkRelation(val from: String,val to:String)


class UrlStatus{}


 object UrlStatus extends Enumeration {

  val InProgress = Value("IN_PROGRESS")
  val Skip = Value("SKIP")
  val Complete = Value("COMPLETE")
  val New = Value("NEW")


}


