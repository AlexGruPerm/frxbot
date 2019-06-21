package mtspredbot

object BotCommandsHelper {
  private val helpText :String =
    """
      |Commands:
      |/info tickerCode -- Ex: info EURUSD
      |/tickers - return list of using tickers (symbols) from dest.
      |/hello
      |/cmd1
      |/select A B
    """.stripMargin

  def getHelpText = helpText

}
