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

  private def simpleRound1Double(valueD : Double) = (valueD * 10).round / 10.toDouble

   val rowToTicker :(Row => Ticker) = (row: Row) =>
    Ticker(
      row.getInt("ticker_id"),
      row.getString("ticker_code")
    )

   val rowToTicksCnt =(row :Row, tickersDict :Seq[Ticker], ticksTotal :Long) => {
    val thisTickerID = row.getInt("ticker_id")
    val cnt = row.getLong("cnt")
    TicksCnt(
      thisTickerID,
      tickersDict.find(_.tickerId == thisTickerID).map(_.tickerCode).getOrElse("n/n"),
      cnt,
      simpleRound1Double(cnt*100L/ticksTotal)
    )
  }

   val rowToBarDateStat = (row :Row, tickersDict :Seq[Ticker]) => {
    val thisTickerID = row.getInt("ticker_id")
    BarDateStat(
      thisTickerID,
      tickersDict.find(_.tickerId == thisTickerID).map(_.tickerCode).getOrElse("n/n"),
      row.getInt("bar_width_sec"),
      row.getLocalDate("ddate")
    )
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

  val prepTicksTotal : BoundStatement = prepareSql(sess, sqlTicksTotal)
  val prepTicksByTicker : BoundStatement = prepareSql(sess, sqlTicksByTicker)
  val prepTicksDistrib : BoundStatement = prepareSql(sess, sqlTicksDistrib)

  val prepBarsAgrStats : BoundStatement = prepareSql(sess, sqlBarsAgrStats)

  private val tickersDict :Seq[Ticker] =  sess.execute(prepTickersDict).all().iterator.asScala.toSeq.map(rowToTicker).toList


  def getTickersDict :Seq[Ticker] = tickersDict/*{
    sess.execute(prepTickersDict).all().iterator.asScala.toSeq.map(rowToTicker).toList
  }*/

  def getMaxDdate(tickerID: Int): LocalDate =
    sess.execute(prepMaxDdateSrc
      .setInt("tickerID", tickerID))
      .one().getLocalDate("ddate")

  def getMaxTs(tickerID: Int, thisDdate: LocalDate): Long =
    sess.execute(prepMaxTsSrc
      .setInt("tickerID", tickerID)
      .setLocalDate("maxDdate", thisDdate))
      .one().getLong("ts")

  def getTicksTotal :Long = sess.execute(prepTicksTotal).one().getLong("cnt")

  def getTicksByTicker(tickerID: Int) :Long =
    sess.execute(prepTicksByTicker
      .setInt("tickerID", tickerID))
      .one().getLong("cnt")

  def getTicksDistrib :Seq[TicksCnt] = sess.execute(prepTicksDistrib).all().iterator.asScala.toSeq
    .map(r => rowToTicksCnt(r,getTickersDict,getTicksTotal)).toList

  def getBarsDdateStats : Seq[BarDateStat] = sess.execute(prepBarsAgrStats).all().iterator.asScala.toSeq
    .map(r => rowToBarDateStat(r,getTickersDict)).toList


}

object CassSessionSrc {
  def apply(configArg :Config):CassSessionSrc = {
    return new CassSessionSrc(configArg)
  }
}
