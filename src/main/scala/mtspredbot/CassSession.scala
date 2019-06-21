package mtspredbot


import java.net.InetSocketAddress
import java.time.LocalDate

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.{BoundStatement, Row}
import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import scala.collection.JavaConverters._
//import scala.collection.JavaConverters._

trait CassSession extends CassQueries {
  def config: Config

  val log = LoggerFactory.getLogger(getClass.getName)

  private val confConnectPath :String = "cassandra."

  def getNodeAddressDc(path :String) :(String,String) =
    (config.getString(confConnectPath+path+".ip"),
      config.getString(confConnectPath+path+".dc"))

  def createSession(node :String,dc :String,port :Int = 9042) :CqlSession =
    CqlSession.builder()
      .addContactPoint(new InetSocketAddress(node, port))
      .withLocalDatacenter(dc)
      .build()

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

class CassSessionSrc(configArg :Config) extends CassSession {
  override val config: Config = configArg

  private val (node: String, dc: String) = getNodeAddressDc("src")
  log.debug("CassSessionSrc address-dc = " + node + " - " + dc)

  def getIpDc: String = node + " - " + dc

  val sess: CqlSession = createSession(node, dc)

  val prepTickersDict: BoundStatement = prepareSql(sess,sqlTickersDict)
  val prepMaxDdateSrc: BoundStatement = prepareSql(sess, sqlMaxDdate)
  val prepMaxTsSrc: BoundStatement = prepareSql(sess, sqlMaxTs)

  val rowToTicker :(Row => Ticker) = (row: Row) =>
    Ticker(
      row.getInt("ticker_id"),
      row.getString("ticker_code")
    )

  def getTickersDict :Seq[Ticker] = {
    sess.execute(prepTickersDict
    ).all().iterator.asScala.toSeq.map(rowToTicker).toList
  }

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
  def apply(configArg :Config):CassSessionSrc = {
    return new CassSessionSrc(configArg)
  }
}