package pl.suu.predictor.stock

import scala.util.Try

trait StockDataProvider {
  def getIntradayStockPrice(fromLastDays: Int, minutesInterval: Int): Try[String]

  def getCurrentStockPrice: Try[String]

  def getFullHistory: Try[String]
}
