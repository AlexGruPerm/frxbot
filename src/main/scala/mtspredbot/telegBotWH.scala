package mtspredbot

import java.time.LocalDate

import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.api.{AkkaTelegramBot, Webhook}
import com.bot4s.telegram.clients.AkkaHttpClient
import com.bot4s.telegram.models.Message
import com.typesafe.config.Config
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

import scala.compat.Platform.EOL
//import java.nio.file.{Path, Files}
//import java.io.File
import scala.concurrent.Future

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
  val webhookPort :Int = config.getInt(confPrefix+"webhook_port")
  val baseUrl = config.getString(confPrefix+"baseUrl")+":"+webhookPort
  override val port = webhookPort
  override val webhookUrl = config.getString(confPrefix+"webhookUrl")
  val client = new AkkaHttpClient(config.getString(confPrefix+"token"))

  /*
  val cfile :java.io.File= new File("C:\\tcert\\mtspredbot.pem")
  val inpCertFilePath :java.nio.file.Path = cfile.toPath
  override val certificate :Option[InputFile] = Option(InputFile(inpCertFilePath))

  def testCert =
   certificate match {
     case Some(c) => log.info("Some - certificate")
     case None => log.info("None - certificate")
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

  onCommand("tickers" ) {
    implicit msg => reply(tickersDict.sortBy(t => t.tickerId).mkString(EOL))
      Future.successful()
  }

  onCommand("help" ) {
    implicit msg => reply(BotCommandsHelper.getHelpText)
      Future.successful()
  }

  override def receiveMessage(msg: Message): Future[Unit] = {
    msg.text.fold(Future.successful(())) {
      text =>
        log.info(s"receiveMessage text OK =$text")
        Future.successful()
    }
  }

}

