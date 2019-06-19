package mtspredbot

object BotCommandsHelper {
  private val helpText :String =
    """
      |/hello
      |/cmd1
      |/tickers - return list of using tickers (symbols) from dest.
      |/info tickerCode -- Example info EURUSD, show information from src,dest.
      |/select A B
    """.stripMargin

  def getHelpText = helpText

}
