package com.nevilon.nomad.filter

import groovy.lang.{GroovyObject, GroovyClassLoader}
import java.io.File
import collection.mutable.ListBuffer
import com.nevilon.nomad.crawler.EntityParams

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/27/13
 * Time: 8:13 AM
 */



class GroovyEntityFilter(groovyFile: File) extends Filter[EntityParams] {

  private val engine = new GroovyFilterEngine[EntityParams]("filterEntity", groovyFile) {
    def mapArgs(t: EntityParams): List[AnyRef] = {
      List(t.size, t.url, t.mimeType).asInstanceOf[List[AnyRef]]
    }
  }


  def filter(entityParams: EntityParams): Option[Action.Action] = {
    engine.filter(entityParams)
  }
}


class GroovyUrlFilter(groovyFile: File) extends Filter[String] {

  private val engine = new GroovyFilterEngine[String]("filterUrl", groovyFile) {
    def mapArgs(t: String): List[AnyRef] = {
      List[AnyRef](t)
    }
  }

  def filter(url: String): Option[Action.Action] = {
    engine.filter(url)
  }

}


abstract class GroovyFilterEngine[T](filterMethodName: String, groovyFile: File) {

  private var groovyObject: GroovyObject = null

  init()

  private def init() {
    val parent = getClass.getClassLoader
    val loader = new GroovyClassLoader(parent)
    //temporary!
    val groovyClass = loader.parseClass(groovyFile)
    groovyObject = groovyClass.newInstance().asInstanceOf[GroovyObject]
  }

  def mapArgs(t: T): List[AnyRef]

  def filter(t: T): Option[Action.Action] = {
    val result = groovyObject.invokeMethod(filterMethodName, mapArgs(t).toArray[AnyRef])
    if (!result.asInstanceOf[Boolean]) {
      Some(Action.Skip)
    } else None
  }
}