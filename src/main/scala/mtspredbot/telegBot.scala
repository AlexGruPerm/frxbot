package mtspredbot
/*
import java.net.{InetSocketAddress, Proxy}

import cats.instances.future._
import cats.syntax.functor._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.clients.ScalajHttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.bot4s.telegram.methods.SendMessage
import com.bot4s.telegram.models._
import com.softwaremill.sttp.okhttp._
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

import scala.concurrent.Future
*/
import java.net.{InetSocketAddress, Proxy}

import cats.instances.future._
import cats.syntax.functor._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.clients.ScalajHttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.bot4s.telegram.methods.SendMessage
import com.softwaremill.sttp.okhttp._
import com.typesafe.config.{Config, ConfigFactory}
import slogging.{LogLevel, LoggerConfig, PrintLoggerFactory}

import scala.concurrent.Future
object SttpBackends{
  val default = OkHttpFutureBackend()
}

//object MyBot extends TelegramBot with Polling with Commands
//object telegBot extends TelegramBot
class telegBot(log :org.slf4j.Logger) extends TelegramBot
  with Polling
  with Commands[Future] {

 // val log = LoggerFactory.getLogger(getClass.getName)
 // LoggerConfig.level = LogLevel.TRACE

  /*
  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE
  */
  LoggerConfig.factory = PrintLoggerFactory()
  LoggerConfig.level = LogLevel.TRACE

  log.info("Begin BOT inside telegBot")

  val config :Config = try {
    ConfigFactory.load()
  } catch {
    case e:Exception =>
      log.error("ConfigFactory.load - cause:"+e.getCause+" msg:"+e.getMessage)
      throw e
  }
  val confPrefix :String = "teleg."
  val botToken :String = config.getString(confPrefix+"token")
  val chatID :Long = config.getLong(confPrefix+"chatid")
  val proxyHost :String = config.getString(confPrefix+"proxy_host")
  val proxyPort :Int =  config.getInt(confPrefix+"proxy_port")

  this.logger.info("BOT TOKEN : "+botToken)

  implicit val backend = SttpBackends.default

  //http://spys.one/proxys/US/
  val proxy = new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(proxyHost, proxyPort))

  override val client = new ScalajHttpClient(botToken, proxy)

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

  onCommand("hello") { implicit msg =>
    using(_.from) { user =>
      log.info("onCommand hello")
      reply("This is a message text to user.firstName=["+user.firstName+"]").void
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
