package mtspredbot

case class TicksCnt(tickerId :Int, tickerCode :String, tickCount :Long, prcnt :Double) {
  override def toString: String =
    tickerCode+" ["+tickerId+"]    "+tickCount+"    - "+prcnt
}
