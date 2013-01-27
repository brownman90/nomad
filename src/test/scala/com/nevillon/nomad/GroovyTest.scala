package com.nevillon.nomad

import org.junit.{Assert, Test}
import groovy.lang.{GroovyClassLoader, GroovyObject}
import java.io.File
import collection.mutable.ListBuffer
import java.util

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/27/13
 * Time: 7:11 AM
 */
class GroovyTest {

  @Test def groovyEvalTest() {
    val parent = getClass.getClassLoader
    val loader = new GroovyClassLoader(parent)
    val groovyClass = loader.parseClass(new File("src/main/scala/com/nevilon/nomad/filter/urlfilter.groovy"))

    /*
    for(m<-groovyClass.getMethods){
      println(m.getName)
    }
    */
    val groovyObject =  groovyClass.newInstance().asInstanceOf[GroovyObject]

    val args = new ListBuffer[Object]
    //val start = System.currentTimeMillis()
    for(i<-1 to 1000000){
      val tmp = groovyObject.invokeMethod("filterUrl", ListBuffer[AnyRef]("from","to").toArray[AnyRef])
      //println(tmp)
    }
    //println(System.currentTimeMillis()-start)
  }
}