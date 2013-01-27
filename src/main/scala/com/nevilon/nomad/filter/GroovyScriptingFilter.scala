package com.nevilon.nomad.filter

import groovy.lang.{GroovyObject, GroovyClassLoader}
import java.io.File
import collection.mutable.ListBuffer

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
    val args = new ListBuffer[Object]
    val result = groovyObject.invokeMethod("filterUrl", ListBuffer[AnyRef](url).toArray[AnyRef])
    if (!result.asInstanceOf[Boolean]) {
      Some(Action.Skip)
    } else None
  }
}