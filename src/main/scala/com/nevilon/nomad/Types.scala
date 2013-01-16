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
  type LinkRelation = (String, String)

}

object UrlStatus extends Enumeration {
  val InProgress = Value("IN_PROGRESS", 0)
  val Skip = Value("SKIP", 1)
  val Complete = Value("COMPLETE", 2)
  val New = Value("NEW", 3)

  class UrlStatus(name: String, val intId: Int) extends Val(nextId, name)

  protected final def Value(name: String, intId: Int): UrlStatus = new UrlStatus(name, intId)

  //UrlStatus.A.id
}


