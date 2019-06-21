package mtspredbot

import java.net.{InetSocketAddress, Proxy}
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date

import cats.instances.future._
import cats.syntax.functor._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.bot4s.telegram.methods.SendMessage
import com.bot4s.telegram.models.Message
import com.softwaremill.sttp.okhttp._
import com.typesafe.config.Config
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

import scala.concurrent.Future
object SttpBackends{
  val default = OkHttpFutureBackend()
}

class telegBot(log :org.slf4j.Logger,
               config :Config,
               sessSrc :CassSessionSrc) extends TelegramBot
  with Polling
  with Commands[Future] {

  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE

  val confPrefix :String = "teleg."
  val botToken :String = config.getString(confPrefix+"token")
  val chatID :Long = config.getLong(confPrefix+"chatid")
  val proxyHost :String = config.getString(confPrefix+"proxy_host")
  val proxyPort :Int =  config.getInt(confPrefix+"proxy_port")
  val userAdmin :Int = config.getInt(confPrefix+"user_admin")

  this.logger.info("BOT TOKEN : "+botToken)

  implicit val backend = SttpBackends.default

  //http://spys.one/proxys/US/
  val proxy = new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(proxyHost, proxyPort))

  override val client :ScalajHttpClientMy = new ScalajHttpClientMy(botToken, proxy)



    def sendMessageTest(msgText :String) :Future[Unit] = {
      request(SendMessage(chatID, msgText)).void
    }


    /*
    override def receiveMessage(msg: Message): Future[Unit] ={
      println("111111111111111 we receive message from "+msg.from )
      msg.text.fold(Future.successful(())) {
        text =>
          {
            request(SendMessage(msg.source, "sendback = "+text)).void
          }
      }
    }
    */
    val tickersDict :Seq[Ticker] = sessSrc.getTickersDict

    onCommand("help" ) {
      implicit msg => reply(BotCommandsHelper.getHelpText).void
    }

    import scala.compat.Platform.EOL

    onCommand("tickers" ) {
      implicit msg => reply(tickersDict.sortBy(t => t.tickerId).mkString(EOL)).void
    }

  /*
    onCommand("hello") { implicit msg =>
      using(_.from) {
        user =>
          log.info("onCommand hello")
          user.id==userAdmin match {
            case true  => reply("Command from ADMIN.").void
            case false => reply("Command from USER ["+user.firstName+" "+user.lastName.getOrElse(" ")+"] with ID=["+user.id+"]").void
          }
      }
    }
  */

    onCommand("close") { implicit msg =>
      using(_.from) {
        user =>
          log.info("onCommand close connections.")
          sessSrc.sess.close()
          reply("Connection closed. Try /check.").void
      }
    }


  onCommand('check) { implicit msg =>
      replyMd(
          sessSrc.sess.isClosed == true match {case true => " disconnected" case false => " connected"}
      ).void
  }

  /*
  just example
  onCommand('check) { implicit msg =>
    withArgs { args =>
      log.info("onCommand ["+msg+"]")
      replyMd(
        if (args.isEmpty)
          "No arguments provided."
        else {
          val dbLocation :String = args(0)
          dbLocation match {
            case "SRC"|"src"|"Src"    => dbLocation + (sessSrc.sess.isClosed==true match {case true => " disconnected" case false => " connected"})
            case _  => "Unknown location ["+dbLocation+"]"
          }
        }
      ).void
    }
  }
  */

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
      //log.info(" SOURCE = "+msg.source) equal msg.chat.id
      //log.info(" CONTACT userId      = "+msg.contact.map(c => c.userId))
      //log.info(" CONTACT phoneNumber = "+msg.contact.map(c => c.phoneNumber))
      log.info(" LOC latitude  = "+msg.location.map(l => l.latitude))
      log.info(" LOC longitude = "+msg.location.map(l => l.longitude))
      log.info(" Chat ID    = "+msg.chat.id)
      //log.info(" Chat Desc  = "+msg.chat.description)
      //log.info(" Chat Title = "+msg.chat.title)
      /**~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
      log.info(" msg date        = "+msg.date)
      //log.info(" msg forwardDate = "+msg.forwardDate)
      log.info(" messageId = "+msg.messageId)
      log.info(" text = "+msg.text.mkString(","))
      log.info(" ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ")
    }

    def onInfoResLog(res :String) ={
      log.info(" ---------------------------------------- ")
      log.info(res)
      log.info(" ---------------------------------------- ")
    }

    def convertLongToDate(l: Long): Date = new Date(l)

    //http://tutorials.jenkov.com/java-internationalization/simpledateformat.html
    // Pattern Syntax
    val DATE_FORMAT = "dd.MM.yyyy HH:mm:ss"

    /**
      * When we convert unix_timestamp to String representation of date and time is using same TimeZone.
      * Later we can adjust it with :
      *
      * val format = new SimpleDateFormat()
      * format.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC"))
      * val dateAsString = format.format(date)
      *
      */
    def getDateAsString(d: Date): String = {
      val dateFormat = new SimpleDateFormat(DATE_FORMAT)
      dateFormat.format(d)
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
        ).void
      }
    }


  onCommand('tickstotal) { implicit msg =>
    replyMd(
      sessSrc.getTicksTotal.toString
    ).void
  }

  onCommand('tickscode) { implicit msg =>
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
              val cnt :Long = sessSrc.getTicksByTicker(tickerId)
              val res :String =
                s"*$tickerCode* TICKSCOUNT $cnt"
              onInfoResLog(res)
              res
            }
            case None => "There is no element with tickerCode ("+ args(0) +") in database. Try command /tickers"
          }
        }
      ).void
    }
  }

  onCommand('ticksall) { implicit msg =>
    onCommandLog(msg)
    val ticksCounts: Seq[TicksCnt] = sessSrc.getTicksDistrib
    //Head of output
    reply("SYMBOL [TickerID] TicksCount Percent").void
    reply(
      ticksCounts.sortBy(t => t.tickCount)(Ordering[Long].reverse).mkString(EOL)
    ).void
  }

  implicit val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  onCommand('barsstat) { implicit msg =>
    onCommandLog(msg)
    val barsStats: Seq[BarDateStat] = sessSrc.getBarsDdateStats
    //val seqTickersId :Seq[Int] = barsStats.map(e => e.tickerId).distinct
    val dist : Seq[BarDateStat] =
      (for(elm <- barsStats.map(e => e.tickerId).distinct) yield {
        barsStats.filter(p => p.tickerId == elm).maxBy(elm => elm.dDate)
      }).sortBy(t => t.dDate.toEpochDay)(Ordering[Long].reverse)

    //Head of output
    reply("SYMBOL [TickerID]  MAXDATE").void
    reply(dist.mkString(EOL)
    ).void
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
        ).void
    }

  /*
//tickscode
//ticksall
  */

  /*
  def getTicksTotal :Long
  def getTicksByTicker(tickerID: Int) :Long =
  def getTicksDistrib :Seq[TicksCnt]
  */

  /*
    onCommand("cmd1" ) {
      log.info("onCommand cmd1")
      implicit msg => reply("This is a message text 1").void
    }

    onCommand('choose | 'pick | 'select) { implicit msg =>
      withArgs { args =>
        log.info("onCommand "+msg)
        replyMd(
          if (args.isEmpty)
            "No arguments provided."
          else
            "SIZE="+args.flatten.size+" arg(0) = "+args(0)+" arg(1) = "+args(1)
        ).void
      }
    }
  */

  }


