package mtspredbot

import java.io.{File, FileInputStream, InputStream}
import java.security.{KeyStore, SecureRandom}
import java.time.LocalDate

import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.api.{AkkaTelegramBot, Webhook}
import com.bot4s.telegram.clients.AkkaHttpClient
import com.bot4s.telegram.models.{InputFile, Message}
import com.typesafe.config.Config
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import oshi.SystemInfo
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

import scala.concurrent.Future

// import scala.compat.Platform.EOL
import scala.compat.Platform.EOL

class telegBotWH(log :org.slf4j.Logger,
                 config :Config,
                 sessSrc :CassSessionSrc)
    extends AkkaTelegramBot
    with Webhook
    with CommonFuncs
    with Commands[Future]
{
  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE

  val tickersDict :Seq[Ticker] = sessSrc.getTickersDict

  val confPrefix :String = "teleg."

  val port :Int = config.getInt(confPrefix+"webhook_port")
  val webhookUrl = config.getString(confPrefix+"webhookUrl")

  log.info(" webhookUrl="+webhookUrl+" port="+port)

  val certPathStr :String = config.getString(confPrefix+"pubcertpath")
  log.info("certificate path ="+certPathStr)

  override def certificate: Option[InputFile] = Some(
    InputFile(new File(certPathStr).toPath)
  )

  override def receiveMessage(msg: Message): Future[Unit] = {
    log.info("receiveMessage method!!!")
    msg.text.fold(Future.successful(())) {
      text =>
        log.info(s"receiveMessage text OK =$text")
        Future.successful()
    }
  }

  val keystorePassword :Array[Char] = config.getString(confPrefix+"keyStorePassword").toCharArray

  override val interfaceIp: String = "0.0.0.0"

  // Set custom context.
  Http().setDefaultServerHttpContext(httpsContext(keystorePassword))

  def httpsContext(keystorePassword : Array[Char]): HttpsConnectionContext = {
    // Manual HTTPS configuration
    val password: Array[Char] = keystorePassword

    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    val keystore: InputStream = new FileInputStream(config.getString(confPrefix+"p12certpath"))

    require(keystore != null, " - Keystore required!")
    ks.load(keystore, password)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)

    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
    val https: HttpsConnectionContext = ConnectionContext.https(sslContext)

    https
  }

  override val client = new AkkaHttpClient(config.getString(confPrefix+"token"))

  /*
  def sendMessageTest(msgText :String) :Future[Unit] = {
    request(SendMessage(322134338L,msgText))
    Future.successful()
  }
  */

  onCommand('tickers) { implicit msg =>
    onCommandLog(msg)
    log.info("onCommand[tickers]")
    Future.successful()
  }

  onCommand('check) { implicit msg =>
    onCommandLog(msg)
    log.info("onCommand[check]")
    Future.successful()
  }

  onCommand('res) { implicit msg =>
    onCommandLog(msg)
    log.info("onCommand[res]")
    Future.successful()
  }

  def onCommandLog(msg :Message) ={
    log.info(" Command ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ")
    /** USER */
    log.info(" User :")
    log.info(" ID = "+msg.from.map(u => u.id).getOrElse(0))
    log.info(" FIRSTNAME = "+msg.from.map(u => u.firstName).getOrElse(" "))
    log.info(" LASTNAME = "+msg.from.map(u => u.lastName.getOrElse(" ")).getOrElse(" "))
    log.info(" USERNAME = "+msg.from.map(u => u.username.getOrElse(" ")).getOrElse(" "))
    log.info(" LANG = "+msg.from.map(u => u.languageCode.getOrElse(" ")).getOrElse(" "))
    /**~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    log.info(" LOC latitude  = "+msg.location.map(l => l.latitude))
    log.info(" LOC longitude = "+msg.location.map(l => l.longitude))
    log.info(" Chat ID    = "+msg.chat.id)
    /**~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
    log.info(" msg date        = "+msg.date)
    log.info(" messageId = "+msg.messageId)
    log.info(" text = "+msg.text.mkString(","))
    log.info(" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ")
  }

  onCommand('mem) { implicit msg =>
    onCommandLog(msg)
    val mb = 1024*1024
    val runtime = Runtime.getRuntime
    replyMd(
      s"""  Used Memory:  ${(runtime.totalMemory - runtime.freeMemory) / mb} Mb
         | Free Memory:   ${runtime.freeMemory / mb} Mb
         | Total Memory:  ${runtime.totalMemory / mb} Mb
         | Max Memory:    ${runtime.maxMemory / mb} Mb
         |""".stripMargin
    )
    Future.successful()
  }

  onCommand('gc) { implicit msg =>
    onCommandLog(msg)
    System.gc()
    System.runFinalization()
    replyMd(s"Call GC".stripMargin)
    Future.successful()
  }

  def onInfoResLog(res :String) ={
    log.info(" ---------------------------------------- ")
    log.info(res)
    log.info(" ---------------------------------------- ")
  }

  onCommand('info) { implicit msg =>
    withArgs { args => //todo: move onCommand log into separate common method
      onCommandLog(msg)
      replyMd(
        if (args.isEmpty)
          "No arguments provided."
        else {
          //todo: check here that input tickerCode correct and exists in mts_meta.tickers.
          tickersDict.find(_.tickerCode.toUpperCase == args(0).toUpperCase).map(_.tickerId) match {
            case Some(tickerId :Int) =>
            {
              val tickerCode :String = args(0)
              val thisTickerMaxDdate :LocalDate = sessSrc.getMaxDdate(tickerId)
              val thisTickerMaxTs :Long = sessSrc.getMaxTs(tickerId,thisTickerMaxDdate)
              val currTimestamp :Long = System.currentTimeMillis
              val datDateTime = getDateAsString(convertLongToDate(thisTickerMaxTs))
              val botDateTime = getDateAsString(convertLongToDate(currTimestamp))
              val diffSeconds = currTimestamp/1000L - thisTickerMaxTs/1000L
              val res :String =
                s"""*$tickerCode* MAXDATE _$thisTickerMaxDdate _
                   | dat = $thisTickerMaxTs   $datDateTime
                   | bot = $currTimestamp   $botDateTime
                   | dif = $diffSeconds sec
                   """.stripMargin
              onInfoResLog(res)
              res
            }
            case None => "There is no element with tickerCode ("+ args(0) +") in database. Try command /tickers"
          }
        }
      )
      Future.successful()
    }
  }

  onCommand('tickstotal) { implicit msg =>
    replyMd(
      sessSrc.getTicksTotal.toString
    )
    Future.successful()
  }

  onCommand('tickscode) { implicit msg =>
    withArgs { args => //todo: move onCommand log into separate common method
      onCommandLog(msg)
      replyMd(
        if (args.isEmpty)
          "No arguments provided."
        else {
          tickersDict.find(_.tickerCode.toUpperCase == args(0).toUpperCase).map(_.tickerId) match {
            case Some(tickerId :Int) =>
            {
              val tickerCode :String = args(0)
              val cnt :Long = sessSrc.getTicksByTicker(tickerId)
              val res :String =
                s"*$tickerCode* TICKSCOUNT $cnt"
              onInfoResLog(res)
              res
            }
            case None => "There is no element with tickerCode ("+ args(0) +") in database. Try command /tickers"
          }
        }
      )
      Future.successful()
    }
  }

  onCommand('ticksall) { implicit msg =>
    onCommandLog(msg)
    val ticksCounts: Seq[TicksCnt] = sessSrc.getTicksDistrib
    reply(ticksCounts.sortBy(t => t.tickCount)(Ordering[Long].reverse).mkString(EOL))
    Future.successful()
  }

  onCommand('ticksallnow) { implicit msg =>
    onCommandLog(msg)
    val maxD :LocalDate = sessSrc.getMaxD
    log.info("maxD = "+maxD)
    val ticksCounts: Seq[TicksCnt] = sessSrc.getTicksDistrib(maxD)
    reply(ticksCounts.sortBy(t => t.tickCount)(Ordering[Long].reverse).mkString(EOL))
    Future.successful()
  }

  //implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)
  /**
    * The full documentation for & and | have a note explaining this behaviour:
    * This method evaluates both a and b, even if the result is already determined after evaluating a.
    */
  onCommand('barsstat) { implicit msg =>
    onCommandLog(msg)
    val barsStats: Seq[BarDateStat] = sessSrc.getBarsDdateStats

    val dist : Seq[BarDateStat] =
      (for(elm <- barsStats.map(_.tickerId).distinct) yield {
        barsStats
          .filter(_.tickerId == elm)
          .maxBy(_.dDate.toEpochDay)
      }).sortBy(t => t.dDate.toEpochDay)(Ordering[Long].reverse)

    //Head of output
    reply("SYMBOL [TickerID]  MAXDATE")
    reply(dist.mkString(EOL))
    Future.successful()
  }

  onCommand('barsstcode) { implicit msg =>
    withArgs { args =>
      onCommandLog(msg)
      replyMd(
        if (args.isEmpty) "No arguments provided."
        else {
          tickersDict.find(_.tickerCode.toUpperCase == args(0).toUpperCase).map(_.tickerId)
          match {
            case Some(tickerId :Int) =>
            {
              val tickerCode :String = args(0)
              val barsStats: Seq[BarDateStat] = sessSrc.getBarsDdateCodeStats(tickerId)

              val dist: Seq[BarDateStat] =
                (for (elm <- barsStats.map(_.barWidthSec).distinct) yield {
                  barsStats.filter(_.barWidthSec == elm).maxBy(_.dDate.toEpochDay)
                }).sortBy(_.barWidthSec)
              dist.mkString(EOL)
            }
            case None =>
              "There is no element with tickerCode ("+ args(0) +") in database. Try command /tickers"
          }
        }
      )
      Future.successful()
    }
  }

  onCommand('author) { implicit msg =>
    onCommandLog(msg)
    replyMd(
      s""" *Yakushev Aleksey*
         | _ugr@bk.ru_
         | _yakushevaleksey@gmail.com_
         | telegram = @AlexGruPerm
         | vk       = https://vk.com/id11099676
         | linkedin = https://www.linkedin.com/comm/in/yakushevaleksey
         | gihub    = https://github.com/AlexGruPerm
         | bigdata  = https://yakushev-bigdata.blogspot.com
         | oracle   = http://yakushev-oracle.blogspot.com
             """.stripMargin
    )
    Future.successful()
  }


  /**
    * return last bars by CODE (for all ddates) per each bws
    */
  onCommand('bars) { implicit msg =>
    withArgs { args =>
      onCommandLog(msg)
      replyMd(
        if (args.isEmpty)
          "No arguments provided."
        else {
          tickersDict.find(_.tickerCode.toUpperCase == args(0).toUpperCase).map(_.tickerId) match {
            case Some(tickerId :Int) =>
            {
              log.info("tickerId = "+tickerId)
              val tickerCode :String = args(0)
              log.info("tickerCode = "+tickerCode)

              val inpBws :Int = if (args.size >= 2)
                args(1).toInt
              else 0

              log.info("inpBws="+inpBws)

              val sqBs : Seq[BarDateStat] = if (inpBws != 0) {
                sessSrc.getBarsDdateCodeStats(tickerId).filter(elm => elm.barWidthSec==inpBws)
              } else {
                sessSrc.getBarsDdateCodeStats(tickerId)
              }

              val sqBarsMaxDates : Seq[BarDateStat] = sqBs.map(t => t.barWidthSec).distinct.map(tbws =>sqBs.filter(b => b.barWidthSec == tbws).maxBy(bf => bf.dDate.toEpochDay))
              val res :Seq[(Int,LocalDate,Long)] = sqBarsMaxDates.map(elm => (elm.barWidthSec,elm.dDate,sessSrc.getMaxTsByDate(tickerId,elm.dDate,elm.barWidthSec)))
              val sqBars :Seq[Bar] = res.map(elm => sessSrc.getBars(tickerId,elm._2,elm._1,elm._3)).flatten

              log.info("sqBars.size = "+sqBars.size)
              if (sqBars.nonEmpty)
                sqBars.sortBy(t => t.barWidthSec).mkString(EOL)
              else
                " There is no last bar."
            }
            case None => "There is no element with tickerCode ("+ args(0) +") in database. Try command /tickers"
          }
        }
      )
      Future.successful()
    }
  }

  onCommand("sys" ) {
    // https://github.com/oshi/oshi/blob/master/oshi-core/src/test/java/oshi/SystemInfoTest.java
    val si = new SystemInfo()
    val hal = si.getHardware()
    //val os = si.getOperatingSystem()
    val cpu = hal.getProcessor
    implicit msg => replyMd(
      s""" ----------------------------------------------------
         | Sys CPU Load: ${cpu.getSystemCpuLoad}
         | isCpu64bit  : ${cpu.isCpu64bit}
         | Model       : ${cpu.getName+" "+cpu.getModel}
         | Phys Cores  : ${cpu.getPhysicalProcessorCount}
         | Log Cores   : ${cpu.getLogicalProcessorCount}
         |---------------------------------------------------- """.stripMargin
    )
      Future.successful()
  }

  onCommand("cpu" ) {
    // https://github.com/oshi/oshi/blob/master/oshi-core/src/test/java/oshi/SystemInfoTest.java
    val cpu = new SystemInfo().getHardware().getProcessor
    implicit msg => replyMd("Sys CPU Load: " +cpu.getSystemCpuLoad)
      Future.successful()
  }



  onCommand("tickers" ) {
    implicit msg => reply(tickersDict.sortBy(t => t.tickerId).mkString(EOL))
      Future.successful()
  }


  onCommand("help" ) {
    implicit msg => reply(BotCommandsHelper.getHelpText)
      Future.successful()
  }

  onCommand("close") { implicit msg =>
    using(_.from) {
      user =>
        log.info("onCommand close connections.")
        sessSrc.sess.close()
        reply("Connection closed. Try /check.")
        Future.successful()
    }
  }

  onCommand('check) { implicit msg =>
    replyMd(
      sessSrc.sess.isClosed == true match {case true => " disconnected" case false => " connected"}
    )
    Future.successful()
  }

}

