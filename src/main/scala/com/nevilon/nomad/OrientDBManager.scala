package com.nevilon.nomad

import com.orientechnologies.orient.client.remote.OServerAdmin
import com.orientechnologies.orient.core.db.graph.OGraphDatabase
import com.orientechnologies.orient.core.metadata.schema.OType
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert
import com.orientechnologies.orient.core.config.OGlobalConfiguration
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx

/**
 * Created with IntelliJ IDEA.
 * User: hudvin
 * Date: 1/11/13
 * Time: 10:00 AM
 */
class OrientDBManager(recreateDatabase: Boolean = false) {

  private val DB_PATH = "remote:/localhost/nomad"
  private val SERVER_CREDENTIALS = ("root", "qwerty")
  private val DB_CREDENTIALS = ("admin", "admin")
  private val NEW_DB_PARAMS = ("graph", "local")


  object UrlType {
    val NAME = "url"
    val LOCATION_FIELD = "location"
    val LOCATION_IDX = "locationIDX"

    //str values
    val STATUS_FIELD = "status"
    val STATUS_FIELD_IDX = "statusFieldIDX"
    //IN_PROGRESS/COMPLETE/SKIP/NEW
  }

  object DomainType {
    val NAME = "domain"
    val NAME_FIELD = "name"
    val NAME_IDX = "nameIDX"
  }

  private var server: OServerAdmin = null
  private var database: OGraphDatabase = null

  if (recreateDatabase) {
    connectToAdmin()
    if (server.existsDatabase()) {
      dropDatabase()
    }
    createDatabase()
    closeAdmin()
    connectToDatabase()
    createSchema()
  } else {
    connectToDatabase()
  }

  //OGlobalConfiguration.MVRBTREE_NODE_PAGE_SIZE.setValue(32768)
//  mvrbtree.ridBinaryThreshold
  OGlobalConfiguration.MVRBTREE_RID_BINARY_THRESHOLD.setValue(-1)
  //database.declareIntent( new OIntentMassiveInsert() )

  private def connectToAdmin() {
    server = new OServerAdmin(DB_PATH).connect(SERVER_CREDENTIALS._1, SERVER_CREDENTIALS._2)
  }

  private def closeAdmin() {
    server.close()
  }

  private def connectToDatabase() {
    database = new OGraphDatabase(DB_PATH)
    database.open(DB_CREDENTIALS._1, DB_CREDENTIALS._2)
    database.setUseCustomTypes(true)
    database.setLockMode(OGraphDatabase.LOCK_MODE.NO_LOCKING)
  }

  def closeDatabase() {
    database.close()
  }

  private def dropDatabase() {
    server.dropDatabase()
  }

  private def createDatabase() {
    server.createDatabase(NEW_DB_PARAMS._1, NEW_DB_PARAMS._2)
  }

  def getDatabase(): OGraphDatabase = {
    database
  }

  private def isVertexTypeExists(vertexType: String): Boolean = {
    database.getVertexType(vertexType) != null
  }

  private def createSchema() {
    //create types
    if (!isVertexTypeExists(UrlType.NAME)) {
      val pageType = database.createVertexType(UrlType.NAME)
      //location field
      pageType.createProperty(UrlType.LOCATION_FIELD, OType.STRING)
      pageType.createIndex(UrlType.LOCATION_IDX, INDEX_TYPE.UNIQUE, UrlType.LOCATION_FIELD)
      //status field
      pageType.createProperty(UrlType.STATUS_FIELD, OType.STRING)
      pageType.createIndex(UrlType.STATUS_FIELD_IDX, INDEX_TYPE.NOTUNIQUE, UrlType.STATUS_FIELD)

    }

    if (!isVertexTypeExists(DomainType.NAME)) {
      val domainType = database.createVertexType(DomainType.NAME)
      domainType.createProperty(DomainType.NAME_FIELD, OType.STRING)
      domainType.createIndex(DomainType.NAME_IDX, INDEX_TYPE.UNIQUE, DomainType.NAME_FIELD)
    }
  }

}
