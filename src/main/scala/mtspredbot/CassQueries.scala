package mtspredbot

trait CassQueries {

  val sqlTickersDict  = "select ticker_id,ticker_code from mts_meta.tickers allow filtering"

  val sqlMaxDdate  = "select max(ddate) as ddate from mts_src.ticks_count_days where ticker_id = :tickerID and ticks_count>0 allow filtering"

  val sqlMaxTs  = "select db_tsunx as ts from mts_src.ticks where ticker_id = :tickerID and ddate=:maxDdate limit 1 allow filtering"

  val sqlTicksTotal  = "select sum(ticks_count) as cnt from mts_src.ticks_count_total"

  val sqlTicksByTicker ="select sum(ticks_count) as cnt from mts_src.ticks_count_total where ticker_id = :tickerID"

  val sqlTicksDistrib = "select ticker_id, sum(ticks_count) as cnt from mts_src.ticks_count_total group by ticker_id"

  val sqlBarsAgrStats = "select distinct ticker_id,bar_width_sec,ddate from mts_bars.bars allow filtering"
}