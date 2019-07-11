package mtspredbot

case class Ticker(tickerId :Int, tickerCode :String, tickerFirst :String, tickerSeconds :String) {
  override def toString = tickerCode + " [" + tickerId + "] "+tickerFirst+" / "+tickerSeconds
}

