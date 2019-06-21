package mtspredbot

trait CassQueries {

  val sqlTickersDict :String = "select ticker_id,ticker_code from mts_meta.tickers allow filtering"

  val sqlMaxDdate :String = "select max(ddate) as ddate from mts_src.ticks_count_days where ticker_id = :tickerID and ticks_count>0 allow filtering"

  val sqlMaxTs :String = "select db_tsunx as ts from mts_src.ticks where ticker_id = :tickerID and ddate=:maxDdate limit 1 allow filtering"

}