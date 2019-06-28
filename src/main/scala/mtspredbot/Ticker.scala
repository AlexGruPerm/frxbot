package mtspredbot

case class Ticker(tickerId :Int, tickerCode :String) {
  override def toString = tickerCode + " [" + tickerId + "]"
}

