package pl.suu.predictor.stock
import java.io.File

import scala.io.Source
import scala.util.Try
import resource.managed

case class LocalDataProvider(stock: String = "KGHA.F") extends StockDataProvider {
  override def getIntradayStockPrice(fromLastDays: Int, minutesInterval: Int): Try[String] = {
    (for {
        result <- managed(Source.fromURL(getClass.getResource(s"$stock-last-7-days.json")))
    } yield result.mkString).tried
  }

  override def getCurrentStockPrice: Try[String] = {
    (for {
      result <- managed(Source.fromURL(getClass.getResource(s"$stock-current.json")))
    } yield result.mkString).tried
  }

  override def getFullHistory: Try[String] = {
    (for {
      result <- managed(Source.fromURL(getClass.getResource(s"$stock-history.json")))
    } yield result.mkString).tried
  }
}
