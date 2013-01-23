package com.nevillon.nomad

import org.junit.{Assert, Test}
import com.nevilon.nomad.crawler.URLUtils

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/11/13
 * Time: 11:59 AM
 */
class URLNormalizerTest {


  @Test
  def normalize(){
    Assert.assertEquals(URLUtils.normalize("http://lenta.ru"),"http://lenta.ru")
    Assert.assertEquals(URLUtils.normalize("www.lenta.ru"),"http://lenta.ru")
    Assert.assertEquals(URLUtils.normalize("www.google.com/"),"http://google.com")
    Assert.assertEquals(URLUtils.normalize("https://www.lenta.ru"),"https://lenta.ru")
    Assert.assertEquals(URLUtils.normalize("www.lenta.ru/"),"http://lenta.ru")
    Assert.assertEquals(URLUtils.normalize("https://www.lenta.ru/index.html#panel#another"),"https://lenta.ru/index.html")
  }



}
