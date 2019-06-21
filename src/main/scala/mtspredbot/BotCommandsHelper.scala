package mtspredbot

object BotCommandsHelper {
  private val helpText :String =
    """
      |Commands:
      |/author - return contact information
      |/info tickerCode -- Ex: info EURUSD
      |/tickers - return list of using tickers (symbols) from dest.
      |/tickstotal - return total ticks count from mts_src.ticks_count_total
      |/tickscode tickerCode - return -----//----- by tickerCode
      |/ticksall - return count of ticks and percents
      |/cmd1
      |/select A B
    """.stripMargin

  def getHelpText = helpText

}
