package com.nevillon.nomad

import org.junit.Test
import java.net.URL
import com.nevilon.nomad.URLUtils

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/11/13
 * Time: 11:59 AM
 */
class URLNormalizerTest {

  /*


   */
  @Test
  def normalize(){
    val url = new URL("https://www.lenta.ru/data/news/")
    println(url.getPath)
    println(URLUtils.normalize("http://lenta.ru"))
    println(URLUtils.normalize("www.lenta.ru"))


    println(URLUtils.normalize("www.google.com/"))
    println(URLUtils.normalize("https://www.lenta.ru"))
    println(URLUtils.normalize("www.lenta.ru/"))
    println(URLUtils.normalize("https://www.lenta.ru/index.html#panel#another"))
  }



}
