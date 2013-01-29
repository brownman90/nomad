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
class GroovyScriptingFilter extends UrlFilter {

  var groovyObject: GroovyObject = null

  init()

  private def init() {
    val parent = getClass.getClassLoader
    val loader = new GroovyClassLoader(parent)
    val path = "src/main/scala/com/nevilon/nomad/filter/urlfilter.groovy"
    val groovyClass = loader.parseClass(new File(path))
    groovyObject = groovyClass.newInstance().asInstanceOf[GroovyObject]
  }

  def filter(url: String): Option[Action.Action] = {
    val result = groovyObject.invokeMethod("filterUrl", ListBuffer[AnyRef](url).toArray[AnyRef])
    if (!result.asInstanceOf[Boolean]) {
      Some(Action.Skip)
    } else None
  }
}


class GroovyEntityFilter extends EntityFilter {

  private val engine = new GroovyFilterEngine[EntityParams]("filterEntity") {
    def mapArgs(t: EntityParams): List[AnyRef] = {
      List(t.size, t.url, t.mimeType).asInstanceOf[List[AnyRef]]
    }
  }


  def filter(entityParams: EntityParams): Option[Action.Action] = {
    engine.filter(entityParams)
  }
}


class GroovyUrlFilter extends UrlFilter {

  private val engine = new GroovyFilterEngine[String]("filterUrl") {
    def mapArgs(t: String): List[AnyRef] = {
      List[AnyRef](t)
    }
  }

  def filter(url: String): Option[Action.Action] = {
    engine.filter(url)
  }

}


abstract class GroovyFilterEngine[T](filterMethodName: String) {

  private var groovyObject: GroovyObject = null

  init()

  private def init() {
    val parent = getClass.getClassLoader
    val loader = new GroovyClassLoader(parent)
    //temporary!
    val path = "src/main/scala/com/nevilon/nomad/filter/urlfilter.groovy"
    val groovyClass = loader.parseClass(new File(path))
    groovyObject = groovyClass.newInstance().asInstanceOf[GroovyObject]
  }

  def mapArgs(t: T): List[AnyRef]

  def filter(t: T): Option[Action.Action] = {
   // val data = List(1, 2, 3)

   // val args = (data.map(_.asInstanceOf[AnyRef]))
   // groovyObject.invokeMethod("echo", args.toArray[AnyRef])


    val result = groovyObject.invokeMethod(filterMethodName, mapArgs(t).map(_.asInstanceOf[AnyRef]).toArray[AnyRef])
    if (!result.asInstanceOf[Boolean]) {
      Some(Action.Skip)
    } else None
  }
}