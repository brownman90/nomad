/**
 * Copyright (C) 2012-2013 Vadim Bartko (vadim.bartko@nevilon.com).
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * See file LICENSE.txt for License information.
 */
package com.nevillon.nomad

import org.junit.{Assert, Test}
import com.nevilon.nomad.crawler.URLUtils


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
