package mtspredbot

import java.time.LocalDate

case class BarDateStat(tickerId :Int, tickerCode :String, barWidthSec :Int, dDate : LocalDate){
  //def toStringFull: String =
  //  tickerCode+" ["+tickerId+"]    "+barWidthSec+" -   "+dDate.toString

  override def toString: String =
    //tickerCode + " - " + dDate.toString
    tickerCode+" ["+tickerId+"]    "+barWidthSec+" -   "+dDate.toString
}