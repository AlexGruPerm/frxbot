package mtspredbot

import com.bot4s.telegram.models.Message
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.api.{AkkaTelegramBot, Webhook}
import com.bot4s.telegram.clients.AkkaHttpClient
import com.typesafe.config.Config
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

import scala.concurrent.Future

abstract class AkkaExampleBot(val token: String) extends AkkaTelegramBot {
  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE

  override val client = new AkkaHttpClient(token)

}



class telegBotWH(log :org.slf4j.Logger,
               config :Config) extends AkkaTelegramBot
    with Webhook
    with CommonFuncs
    with Commands[Future]
{
  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE

  val confPrefix :String = "teleg."
  val botToken :String = config.getString(confPrefix+"token")
  val chatID :Long = config.getLong(confPrefix+"chatid")
  this.logger.info("BOT TOKEN : "+botToken)

  val baseUrl = "http://193.124.112.90:8443"
  val client = new AkkaHttpClient(botToken)
  override val port = 8443
  override val webhookUrl = "https://59913958.ngrok.io"

  override def receiveMessage(msg: Message): Future[Unit] = {
    msg.text.fold(Future.successful(())) {
      text =>
        log.info(s"receiveMessage  text=$text")
        Future.successful()
    }
  }



}


