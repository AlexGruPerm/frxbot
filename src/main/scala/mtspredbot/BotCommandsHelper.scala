package mtspredbot

object BotCommandsHelper {
  private val helpText :String =
    """
      |Commands:
      |/info tickerCode -- Ex: info EURUSD
      |
      |/hello
      |/cmd1
      |/tickers - return list of using tickers (symbols) from dest.
      |/select A B
    """.stripMargin

  def getHelpText = helpText

}
