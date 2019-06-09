package pl.suu.predictor.stock.model

case class HistoryData(name: String, history: Map[String, StockValues])
