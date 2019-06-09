package pl.suu.predictor.stock
import scala.io.Source
import scala.util.Try

import resource._

case class LocalDataProvider() extends StockDataProvider {
  override def getIntradayStockPrice(fromLastDays: Int, minutesInterval: Int): Try[String] = {
    (for {
      result <- managed(Source.fromURL(getClass.getResource("kghm-last-7-days.json")))
    } yield result.mkString).tried
  }

  override def getCurrentStockPrice: Try[String] = {
    (for {
      result <- managed(Source.fromURL(getClass.getResource("kghm-last-7-days.json")))
    } yield result.mkString).tried
  }

  override def getFullHistory: Try[String] = {
    (for {
      result <- managed(Source.fromURL(getClass.getResource("kghm-history.json")))
    } yield result.mkString).tried
  }
}
