package pl.suu.predictor.stock

import net.liftweb.json._
import pl.suu.predictor.stock.model.{HistoryData, IntradayData, StockData}

import scala.util.{Failure, Success, Try}

case class StockDataService(remoteService: StockDataProvider) {
  implicit val formats: DefaultFormats = DefaultFormats


  def getStockFromLast7Days: Try[IntradayData] = {
    remoteService.getIntradayStockPrice(7, 1) match {
      case Success(stock) => Success(parse(stock).extract[IntradayData])
      case Failure(e) => Failure(e)
    }
  }

  def getFullHistory: Try[HistoryData] = {
    remoteService.getFullHistory match {
      case Success(stock) => Success(parse(stock).extract[HistoryData])
      case Failure(e) => Failure(e)
    }
  }

  def getCurrentStock: Try[StockData] = {
    remoteService.getCurrentStockPrice match {
      case Success(stock) => Success(parse(stock).extract[StockData])
      case Failure(e) => Failure(e)
    }
  }
}
