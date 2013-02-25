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

import com.google.common.net.InternetDomainName

object DomainTest {

  def main(args:Array[String]){
    val fullDomainName = InternetDomainName.from("www.lenta.ru/goo/fafa/iin")
    val publicDomainName = fullDomainName.publicSuffix()
    println(fullDomainName)
    println(publicDomainName)
  }

}
