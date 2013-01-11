package com.nevilon.nomad

import com.google.common.net.InternetDomainName

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/10/13
 * Time: 9:13 AM
 * To change this template use File | Settings | File Templates.
 */
object DomainTest {

  def main(args:Array[String]){
    val fullDomainName = InternetDomainName.from("www.lenta.ru/goo/fafa/iin")
    val publicDomainName = fullDomainName.publicSuffix()
    println(fullDomainName)
    println(publicDomainName)
  }

}
