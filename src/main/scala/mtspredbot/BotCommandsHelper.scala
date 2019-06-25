package mtspredbot

object BotCommandsHelper {
  private val helpText :String =
    """
      |Commands:
      |/author - return contact information
      |/info CODE -- Ex: info EURUSD
      |/tickers - return list of using tickers (symbols) from dest.
      |/~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
      |/tickstotal - return total ticks count from mts_src.ticks_count_total
      |/tickscode CODE - return -----//----- by tickerCode
      |/ticksall - return count of ticks and percents
      |/ticksallnow - return count of ticks and percents for current day
      |/~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
      |/barsstat - return stats by all tickers
      |/barsstcode X - return stats by tickerCode
      |/bars CODE - return last bars for each BWS
      |/bars CODE BWS - return last bar by BSW
      |/~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
      |/cmd1
    """.stripMargin

  def getHelpText = helpText

}
