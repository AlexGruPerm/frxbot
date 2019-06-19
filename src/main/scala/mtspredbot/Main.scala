package mtspredbot

import java.io

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.reflect.io.File

object Main extends App {
  val log = LoggerFactory.getLogger(getClass.getName)
  log.info("~~~~~~~~~~~~~~~~~~~~~~~~ BEGIN TELEGRAM BOT ~~~~~~~~~~~~~~~~~~~~~~~~")

  val config :Config = try {
    if (args.length == 0) {
      log.info("There is no external config file.")
      ConfigFactory.load()
    } else {
      val configFilename :String = System.getProperty("user.dir")+File.separator+args(0)
      log.info("There is external config file, path="+configFilename)
      val fileConfig :Config = ConfigFactory.parseFile(new io.File(configFilename))
      ConfigFactory.load(fileConfig)
    }

  } catch {
    case e:Exception =>
      log.error("ConfigFactory.load - cause:"+e.getCause+" msg:"+e.getMessage)
      throw e
  }

  val bot = new telegBot(log, config)
  val eol = bot.run

  bot.sendMessageTest(
    """
       |Bot "mtsPredBot" started and ready to communicate.
       |You can use command /help to see possible commands.
    """.stripMargin)


  println("Press [ENTER] to shutdown the bot, it may take a few seconds...")
  scala.io.StdIn.readLine()
  log.info("~~~~~~~~~~~~~~~~~~~~~~~~ SHUTDOWN TELEGRAM BOT ~~~~~~~~~~~~~~~~~~~~~~~~")
  bot.shutdown()
  Await.result(eol, Duration.Inf)
}
