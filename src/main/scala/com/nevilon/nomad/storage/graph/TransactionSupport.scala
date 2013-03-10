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
package com.nevilon.nomad.storage.graph

import com.thinkaurelius.titan.core.{TitanGraph, TitanTransaction}
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion

trait TransactionSupport {

  def withTransaction[T](f: TitanTransaction => T)(implicit implGraph: TitanGraph): T = {
    val tx = implGraph.startTransaction()
    try {
      val result = f(tx)
      tx.stopTransaction(Conclusion.SUCCESS)
      result
    }
    catch {
      case ex: Throwable => {
        ex.printStackTrace()
        tx.abort()
        throw ex
      }
    }
  }

}
