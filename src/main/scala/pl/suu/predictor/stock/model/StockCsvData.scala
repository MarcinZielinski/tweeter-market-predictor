package pl.suu.predictor.stock.model

case class StockCsvData(date: String, time: String, open: String, high: String, low: String, close: String,
                        volume: String)

object StockCsvData {
  def from(a: Array[String]): StockCsvData = StockCsvData(a(0), a(1), a(2), a(3), a(4), a(5), a(6))
}