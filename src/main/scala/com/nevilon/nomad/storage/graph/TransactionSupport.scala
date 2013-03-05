package com.nevilon.nomad.storage.graph

import com.thinkaurelius.titan.core.{TitanGraph, TitanTransaction}
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 3/5/13
 * Time: 2:42 PM
 */

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
