package mtspredbot

object BotCommandsHelper {
  private val helpText :String =
    """
      |Used abbreviation:
      |SRC - Source cassandra database
      |DEST - Destination cassandra database
      |A,B  - Ticker code (like EURUSD, USDCHF)
      |Commands (api description):
      |/check X -- Ex: check SRC, check DEST
      |/info tickerCode -- Ex: info EURUSD
      |:show information from src,dest Last DDATE last TS and Diffs.
      |
      |/hello
      |/cmd1
      |/tickers - return list of using tickers (symbols) from dest.
      |/select A B
    """.stripMargin

  def getHelpText = helpText

}
