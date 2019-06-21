package mtspredbot


import java.io.File
import java.net.InetSocketAddress
import java.time.LocalDate

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.config.DriverConfigLoader
import com.datastax.oss.driver.api.core.cql.{BoundStatement, Row}
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

trait CassSession extends CassQueries {
  def config: Config
  def file :File

  val log = LoggerFactory.getLogger(getClass.getName)

  private val confConnectPath :String = "cassandra."

  def getNodeAddressDc(path :String) :(String,String) =
    (config.getString(confConnectPath+path+".ip"),
      config.getString(confConnectPath+path+".dc"))

  def createSession(node :String,dc :String,port :Int = 9042) :CqlSession = {
    log.info("createSession")
    log.info("AbsolutePath = "+file.getAbsolutePath)
    log.info("Name="+file.getName)
    log.info("CanonicalPath="+file.getCanonicalPath)
    log.info("AbsoluteFile="+file.getAbsoluteFile)
    log.info("AbsolutePath="+file.getAbsolutePath)
    log.info("ParentFile="+file.getParentFile)

    //v1
  /*
    val loader =
      DriverConfigLoader.programmaticBuilder()
        .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(30))
        .withDuration(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, Duration.ofSeconds(30))
        .build()

    CqlSession.builder()
      .addContactPoint(new InetSocketAddress(node, port))
      .withConfigLoader(loader)
      .withLocalDatacenter(dc)
      .build()
*/

    CqlSession.builder()
      .addContactPoint(new InetSocketAddress(node, port))
      .withConfigLoader(DriverConfigLoader.fromFile(file))
      .withLocalDatacenter(dc)
      .build()

  }

  //todo: add here try except for misprinting sqls!
  def prepareSql(sess :CqlSession,sqlText :String) :BoundStatement =
    try {
      sess.prepare(sqlText).bind()
    }
    catch {
      case e: com.datastax.oss.driver.api.core.servererrors.SyntaxError =>
        log.error(" prepareSQL - "+e.getMessage)
        throw e
    }
}


class CassSessionSrc(configArg :Config, fileArg :File) extends CassSession {
  override val config: Config = configArg
  override val file: File = fileArg

  private val (node: String, dc: String) = getNodeAddressDc("src")
  log.debug("CassSessionSrc address-dc = " + node + " - " + dc)

  def getIpDc: String = node + " - " + dc

  val sess: CqlSession = createSession(node, dc)

  val prepMaxDdateSrc: BoundStatement = prepareSql(sess, sqlMaxDdate)
  val prepMaxTsSrc: BoundStatement = prepareSql(sess, sqlMaxTs)

  def getMaxDdate(tickerID: Int): LocalDate =
    sess.execute(prepMaxDdateSrc
      .setInt("tickerID", tickerID))
      .one().getLocalDate("ddate")

  def getMaxTs(tickerID: Int, thisDdate: LocalDate): Long =
    sess.execute(prepMaxTsSrc
      .setInt("tickerID", tickerID)
      .setLocalDate("maxDdate", thisDdate))
      .one().getLong("ts")

}

object CassSessionSrc {
  def apply(configArg :Config, fileArg :File):CassSessionSrc = {
    //Thread.sleep(3000)
    return new CassSessionSrc(configArg,fileArg)
  }
}

  class CassSessionDest(configArg :Config, fileArg :File) extends CassSession{
    override val config :Config = configArg
    override val file: File = fileArg

    private val (node: String, dc: String) = getNodeAddressDc("dest")
    log.debug("CassSessionDest address-dc = " + node + " - " + dc)

    def getIpDc: String = node + " - " + dc

    val sess: CqlSession = createSession(node, dc)

    val prepTickerId: BoundStatement = prepareSql(sess, sqlTickerId)
    val prepTickersDict: BoundStatement = prepareSql(sess,sqlTickersDict)
    val prepMaxDdateDest: BoundStatement = prepareSql(sess, sqlMaxDdate)
    val prepMaxTsDest: BoundStatement = prepareSql(sess, sqlMaxTs)

    def getTickerIDByCode(tickerCode :String) :Option[Int] =
      Option(sess.execute(prepTickerId
        .setString("tickerCode", tickerCode))
        .one().getInt("ticker_id"))

    val rowToTicker :(Row => Ticker) = (row: Row) =>
      Ticker(
        row.getInt("ticker_id"),
        row.getString("ticker_code")
      )

    def getTickersDict :Seq[Ticker] = {
      sess.execute(prepTickersDict
      ).all().iterator.asScala.toSeq.map(rowToTicker).toList
    }

    def getMaxExistDdateDest(tickerId: Int) :LocalDate =
      sess.execute(prepMaxDdateDest
        .setInt("tickerID", tickerId))
        .one().getLocalDate("ddate")

    def getMaxTsBydateDest(tickerId: Int, thisDate: LocalDate): Long =
      sess.execute(prepMaxTsDest
        .setInt("tickerID", tickerId)
        .setLocalDate("maxDdate", thisDate))
        .one().getLong("ts")

}

object CassSessionDest {
  def apply(configArg :Config, fileArg :File):CassSessionDest = {
    //Thread.sleep(3000)
    return new CassSessionDest(configArg,fileArg)
  }
}