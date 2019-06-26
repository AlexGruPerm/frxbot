package mtspredbot

trait CassQueries {

  val sqlTickersDict  = "select ticker_id,ticker_code from mts_meta.tickers allow filtering"

  val sqlMaxD = "select max(ddate) as ddate from mts_src.ticks_count_days"

  val sqlMaxDdate  = "select max(ddate) as ddate from mts_src.ticks_count_days where ticker_id = :tickerID and ticks_count>0 allow filtering"

  val sqlMaxTs  = "select db_tsunx as ts from mts_src.ticks where ticker_id = :tickerID and ddate = :maxDdate limit 1 allow filtering"

  val sqlTicksTotal  = "select sum(ticks_count) as cnt from mts_src.ticks_count_total"

  val sqlTicksTotalDay  = "select sum(ticks_count) as cnt from mts_src.ticks_count_days where ddate=:thisDate allow filtering"

  val sqlTicksByTicker ="select sum(ticks_count) as cnt from mts_src.ticks_count_total where ticker_id = :tickerID"

  val sqlTicksDistrib = "select ticker_id, sum(ticks_count) as cnt from mts_src.ticks_count_total group by ticker_id"

  val sqlTicksDistribMaxD = "select ticker_id, sum(ticks_count) as cnt from mts_src.ticks_count_days where ddate=:thisDate group by ticker_id allow filtering"

  val sqlBarsAgrStats = "select ticker_id,bar_width_sec,ddate from mts_bars.bars_bws_dates"
    //"select distinct ticker_id,bar_width_sec,ddate from mts_bars.bars allow filtering"

  val sqlBarsCodeStats = "select ticker_id,bar_width_sec,ddate from mts_bars.bars_bws_dates where ticker_id = :tickerID"
    //"select distinct ticker_id,bar_width_sec,ddate from mts_bars.bars where ticker_id=:tickerID allow filtering"

  val sqlMaxTsEndByDate = "select ts_end as ts from mts_bars.bars where ticker_id=:tickerID and bar_width_sec=:bws and ddate=:thisDate limit 1"
    //"select max(ts_end) as ts from mts_bars.bars where ticker_id=:tickerID and bar_width_sec=:bws and ddate=:thisDate allow filtering"

  val sqlBarByDateTs = "select ticker_id,ddate,bar_width_sec,ts_begin,ts_end,btype,o,c,ticks_cnt from mts_bars.bars where ticker_id=:tickerID and bar_width_sec=:bws and ddate=:thisDate and ts_end=:ts allow filtering"

  //val sqlBarByDateTs = "select ticker_id,ddate,bar_width_sec,ts_begin,ts_end,btype,o,c,ticks_cnt from mts_bars.bars where ticker_id=1 and bar_width_sec=600 and ddate='2019-04-02' and ts_end=1554237893268 allow filtering"

}