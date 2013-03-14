package com.nevilon.nomad.crawler

import crawlercommons.fetcher.UserAgent
import com.nevilon.nomad.boot.GlobalConfig


object UserAgentProvider {

  private val userAgentConfig = GlobalConfig.userAgentConfig

  private val userAgent = new UserAgent(userAgentConfig.name, userAgentConfig.email, userAgentConfig.page)

  def getUserAgent() = userAgent

  def getUserAgentString() = userAgent.getUserAgentString

}
