package mtspredbot

import java.io

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.reflect.io.File

object DEV extends App {
  val log = LoggerFactory.getLogger(getClass.getName)

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


   val tickersFrx = sessSrc.getTickersDict.filter(t => t.tickerSeconds!="RUB" && t.tickerCode!="_DXY")

   case class ValidLastBarRes(thisTicker :Ticker, bws :Int, diffSeconds :Long, lastBar :Option[Bar], ticksMaxTs :Long)
     extends CommonFuncs{
     override def toString: String =
       "["+bws+"] "+thisTicker.tickerCode+ "["+thisTicker.tickerId+"] dif: " + diffSeconds +"sec.    " +
         (lastBar match {
           case Some(lb)=> lb.tsEnd+"  "+getDateAsString(convertLongToDate(lb.tsEnd))
           case None => "___"
         })+"    LastTick = "+getDateAsString(convertLongToDate(ticksMaxTs))+"  "+ticksMaxTs
  }

   def validateLastBars(thisTicker :Ticker, bws :Int) :ValidLastBarRes = {
       sessSrc.getBarsDdateCodeStats(thisTicker.tickerId)
         .filter(lb => lb.barWidthSec == bws).headOption
       match {
         case Some(lb) => {
           val lBar :Option[Bar] = sessSrc.getBars(thisTicker.tickerId,lb.dDate,bws,
                                                   sessSrc.getMaxTsByDate(thisTicker.tickerId,lb.dDate,bws)
                                                  ).headOption
           val ticksMaxTs :Long = sessSrc.getMaxTs(thisTicker.tickerId,lb.dDate)
           ValidLastBarRes(thisTicker,bws,(ticksMaxTs - lBar.map(_.tsEnd).getOrElse(0L))/1000L, lBar,ticksMaxTs)
         }
         case None => ValidLastBarRes(thisTicker,bws,-1L,None,0L)
     }
   }

  tickersFrx.map(validateLastBars(_,30)).sortBy(b => b.diffSeconds)(Ordering[Long].reverse).foreach(bv => log.info(bv.toString))

  /*
Seq("EUR","AUD","GBP","USD")
  .flatMap(elm => tickersFrx.filter(t => t.tickerFirst==elm).sortBy(st => st.tickerFirst))
  .foreach(t => log.info(t.tickerFirst+" "+t.tickerSeconds+" "+t.tickerId))

log.info("-----------------------------------")

 Seq("USD","CAD","CHF","JPY")
   .flatMap(elm => tickersFrx.filter(t => t.tickerSeconds==elm).sortBy(st => st.tickerFirst))
   .foreach(t => log.info(t.tickerFirst+" "+t.tickerSeconds+" "+t.tickerId))
*/
  sessSrc.sess.close()
}
