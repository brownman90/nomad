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
