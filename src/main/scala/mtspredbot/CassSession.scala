package mtspredbot


import java.net.InetSocketAddress
import java.time.LocalDate

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.BoundStatement
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

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
      .withLocalDatacenter(dc).build()

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

  def getIpDc :String = node+"("+dc+")"

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
  def apply(configArg :Config):CassSessionSrc = {
    return new CassSessionSrc(configArg)
  }
}

  class CassSessionDest(configArg :Config) extends CassSession{
    override val config :Config = configArg

  private val (node :String,dc :String) = getNodeAddressDc("dest")
    log.debug("CassSessionDest address-dc = "+node+" - "+dc)

    def getIpDc :String = node+"("+dc+")"

    val sess :CqlSession = createSession(node,dc)

    val prepMaxDdateDest :BoundStatement = prepareSql(sess,sqlMaxDdate)
    val prepMaxTsDest :BoundStatement = prepareSql(sess,sqlMaxTs)

  def getMaxExistDdateDest(tickerId :Int) :LocalDate =
    sess.execute(prepMaxDdateDest
      .setInt("tickerID",tickerId))
      .one().getLocalDate("ddate")

  def getMaxTsBydateDest(tickerId :Int, thisDate :LocalDate) :Long =
    sess.execute(prepMaxTsDest
      .setInt("tickerID",tickerId)
      .setLocalDate("maxDdate",thisDate))
      .one().getLong("ts")

}

object CassSessionDest {
  def apply(configArg :Config):CassSessionDest = {
    return new CassSessionDest(configArg)
  }
}