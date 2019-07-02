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

  val sessSrc :CassSessionSrc =
    try {
      CassSessionSrc.apply(config)
    } catch {
      case s: com.datastax.oss.driver.api.core.servererrors.SyntaxError => {
        log.error("[0] ERROR when get CassSessionXXX SyntaxError - msg="+s.getMessage+" cause="+s.getCause)
        throw s
      }
      case c: CassConnectException => {
        log.error("[1] ERROR when get CassSessionXXX ["+c.getMessage+"] Cause["+c.getCause+"]")
        throw c
      }
      case de : com.datastax.oss.driver.api.core.DriverTimeoutException =>
        log.error("[2] ERROR when get CassSessionXXX ["+de.getMessage+"] ["+de.getCause+"] "+de.getExecutionInfo.getErrors)
        throw de
      case ei : java.lang.ExceptionInInitializerError =>
        log.error("[3] ERROR when get CassSessionXXX ["+ei.getMessage+"] ["+ei.getCause+"] "+ei.printStackTrace())
        throw ei
      case e: Throwable =>
        log.error("[4] ERROR when get CassSessionXXX ["+e.getMessage+"] class=["+e.getClass.getName+"]")
        throw e
    }

  //Webhook

  val bot = new telegBotWH(log, config/*, sessSrc*/)
  val eol = bot.run

  def SeesionConnectedCheck(isClosed :Boolean) :String =
    !isClosed==true match {case true => "connected" case false => "disconnected"}

  println("Press [ENTER] to shutdown the bot, it may take a few seconds...")
  scala.io.StdIn.readLine()
  log.info("~~~~~~~~~~~~~~~~~~~~~~~~ SHUTDOWN TELEGRAM BOT ~~~~~~~~~~~~~~~~~~~~~~~~")
  //sessSrc.sess.close()
  bot.shutdown()
  Await.result(eol, Duration.Inf)
}
