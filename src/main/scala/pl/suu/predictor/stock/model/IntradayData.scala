package pl.suu.predictor.stock.model

case class IntradayData(symbol: String, stock_exchange_short: String, timezone_name: String, intraday: Map[String, StockValues])

case class StockValues(open: String, close: String, high: String, low: String, volume: String)
