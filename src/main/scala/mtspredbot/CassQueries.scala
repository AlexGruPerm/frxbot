package mtspredbot

trait CassQueries {

  val sqlTickersDict :String = "select ticker_id,ticker_code from mts_meta.tickers allow filtering"

  val sqlTickerId :String = "select ticker_id from mts_meta.tickers where ticker_code=:tickerCode allow filtering"

  /**-------------------------------------------------------------*/

  val sqlFirstDdateTick :String = "select min(ddate) as ddate from mts_src.ticks_count_days where ticker_id = :tickerID"

  val sqlFirstTsFrom :String = "select min(db_tsunx) as ts from mts_src.ticks where ticker_id = :tickerID and ddate = :minDdate allow filtering"

  val sqlMaxDdate :String = "select max(ddate) as ddate from mts_src.ticks_count_days where ticker_id = :tickerID and ticks_count>0 allow filtering"

  val sqlMaxTs :String = "select db_tsunx as ts from mts_src.ticks where ticker_id = :tickerID and ddate=:maxDdate limit 1 allow filtering"


  /**
    * All the time read whole dataset for one partition (tickerID+readDate)
    * from db_tsunx > fromTs to the end.
    */
  val sqlReadTicks :String =
    """
      select *
        from mts_src.ticks
       where ticker_id = :tickerID and
             ddate     = :readDate and
             db_tsunx > :fromTs
       allow filtering """

  val sqlReadTicksWholeDate :String =
    """
      select *
        from mts_src.ticks
       where ticker_id = :tickerID and
             ddate     = :readDate
       allow filtering """
}