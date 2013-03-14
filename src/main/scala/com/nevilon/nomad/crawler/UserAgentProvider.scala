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
package com.nevilon.nomad.crawler

import crawlercommons.fetcher.UserAgent
import com.nevilon.nomad.boot.GlobalConfig


object UserAgentProvider {

  private val userAgentConfig = GlobalConfig.userAgentConfig

  private val userAgent = new UserAgent(userAgentConfig.name, userAgentConfig.email, userAgentConfig.page)

  def getUserAgent() = userAgent

  def getUserAgentString() = userAgent.getUserAgentString

}
