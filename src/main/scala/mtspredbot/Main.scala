package mtspredbot

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends App {
  val log = LoggerFactory.getLogger(getClass.getName)
  log.info("~~~~~~~~~~~~~~~~~~~~~~~~ BEGIN TELEGRAM BOT MTSPREDBOT ~~~~~~~~~~~~~~~~~~~~~~~~")

  val config :Config = try {
    ConfigFactory.load()
  } catch {
    case e:Exception =>
      log.error("ConfigFactory.load - cause:"+e.getCause+" msg:"+e.getMessage)
      throw e
  }

  val botToken :String = config.getString("teleg.token")
  log.info("BOT TOKEN : "+botToken)


  val bot = new telegBot(botToken)
  val eol = bot.run

  /*
  val eol = telegBot.run()
  telegBot.sendMessageTest
  */

  println("Press [ENTER] to shutdown the bot, it may take a few seconds...")
  scala.io.StdIn.readLine()

  //bot.shutdown()
  Await.result(eol, Duration.Inf)
}
