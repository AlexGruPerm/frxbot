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
import com.softwaremill.sttp.okhttp._
import com.typesafe.config.Config
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

import scala.concurrent.Future
object SttpBackends{
  val default = OkHttpFutureBackend()
}

class telegBot(log :org.slf4j.Logger,
               config :Config,
               sessSrc :CassSessionSrc,
               sessDest :CassSessionDest) extends TelegramBot
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
    val tickersDict :Seq[Ticker] = sessDest.getTickersDict

    onCommand("help" ) {
      implicit msg => reply(BotCommandsHelper.getHelpText).void
    }

    import scala.compat.Platform.EOL

    onCommand("tickers" ) {
      implicit msg => reply(
        tickersDict.sortBy(t => t.tickerId).mkString(EOL)
      ).void
    }

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

    onCommand("close") { implicit msg =>
      using(_.from) {
        user =>
          log.info("onCommand close connections.")
          sessSrc.sess.close()
          sessDest.sess.close()
          reply("Connection closed. Try /check.").void
      }
    }


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
              case "DEST"|"dest"|"Dest" => dbLocation + (sessDest.sess.isClosed==true match {case true => " disconnected" case false => " connected"})
              case _  => "Unknown location ["+dbLocation+"]"
            }
          }
        ).void
      }
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
        log.info(" ##########  onCommand ["+msg+"]")
        replyMd(
          if (args.isEmpty)
            "No arguments provided."
          else {
            //todo: check here that input tickerCode correct and exists in mts_meta.tickers.
            val userTickerCode :String = args(0).toUpperCase
            val tickerIdOpt :Option[Int] = tickersDict.find(_.tickerCode.toUpperCase == userTickerCode).map(_.tickerId)
            log.info(" tickerIdOpt =["+tickerIdOpt+"] for "+userTickerCode)
            tickerIdOpt match {
              case Some(tickerId :Int) =>
              {
                val tickerCode :String = args(0)
                val thisTickerMaxDdate :LocalDate = sessSrc.getMaxDdate(tickerId)
                val thisTickerMaxTs :Long = sessSrc.getMaxTs(tickerId,thisTickerMaxDdate)
                val currTimestamp :Long = System.currentTimeMillis
                val datDateTime = getDateAsString(convertLongToDate(thisTickerMaxTs))
                val botDateTime = getDateAsString(convertLongToDate(currTimestamp))
                val diffSeconds = currTimestamp/1000L - thisTickerMaxTs/1000L
                s"""$tickerCode  MAXDATE  $thisTickerMaxDdate
                   | datTS = [$thisTickerMaxTs] $datDateTime
                   | botTS = [$currTimestamp] $botDateTime
                   | diff  = $diffSeconds seconds.
                   """.stripMargin
              }
              case None => "There is no element with tickerCode ("+ args(0) +") in database. Try command /tickers"
            }
          }
        ).void
      }
    }

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
  }


