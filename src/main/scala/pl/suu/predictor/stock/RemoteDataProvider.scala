package pl.suu.predictor.stock

import scala.io.Source
import resource._

import scala.util.Try

case class RemoteDataProvider(stock: String = "KGHA.F") extends StockDataProvider {

  def getIntradayStockPrice(fromLastDays: Int, minutesInterval: Int): Try[String] = {
    (for {
      response <- managed(Source.fromURL(s"https://intraday.worldtradingdata.com/api/v1/intraday?symbol=$stock&" +
        s"range=$fromLastDays&interval=$minutesInterval&" +
        s"api_token=${System.getenv("WTD_TOKEN")}"))
    } yield {
      response.mkString
    }).tried
  }

  def getCurrentStockPrice: Try[String] = {
    (for {
      response <- managed(Source.fromURL(s"https://api.worldtradingdata.com/api/v1/stock?symbol=$stock&" +
        s"api_token=${System.getenv("WTD_TOKEN")}"))
    } yield {
      response.mkString
    }).tried
  }

  def getFullHistory: Try[String] = {
    (for {
      response <- managed(Source.fromURL(s"https://api.worldtradingdata.com/api/v1/history?symbol=$stock&sort=newest&" +
        s"api_token=${System.getenv("WTD_TOKEN")}"))
    } yield {
      response.mkString
    }).tried
  }
}
