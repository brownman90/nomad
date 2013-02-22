package com.nevillon.nomad

import org.junit.Test
import com.nevilon.nomad.storage.graph.FileStorage
import com.nevilon.nomad.boot.GlobalConfig

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/31/13
 * Time: 1:32 PM
 */
class FileStorageTest {

  @Test def gridFsTest() {
    val fileStorage = new FileStorage(GlobalConfig.mongoDBConfig)
    //fileStorage.connect()
    //Prototypes.timed(fileStorage.testGridFs())

  }

}
