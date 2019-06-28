package mtspredbot

import java.time.LocalDate

case class Bar(
                tickerId :Int,
                tickerCode :String,
                ddate :LocalDate,
                barWidthSec :Int,
                tsBegin :Long,
                tsEnd :Long,
                bType :String,
                o :Double,
                c :Double,
                ticksCnt :Int
              ) extends CommonFuncs
{
  override def toString: String =
    ddate+" ["+barWidthSec+"]"+bType+"  *"+ getDateAsString(convertLongToDate(tsEnd)) +"* "+" c:"+c+" tc:"+ticksCnt

}

