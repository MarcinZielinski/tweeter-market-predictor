package pl.suu.predictor.stock

import net.liftweb.json._
import pl.suu.predictor.stock.model.{HistoryData, IntradayData, StockData}

import scala.util.{Failure, Success}

case class StockService(remoteService: StockDataProvider) {
  implicit val formats: DefaultFormats = DefaultFormats


  def getStockFromLast7Days: IntradayData = {
    remoteService.getIntradayStockPrice(7, 1) match {
      case Success(stock) => parse(stock).extract[IntradayData]
      case Failure(e) => sys.error(e.getLocalizedMessage)
    }
  }

  def getFullHistory: HistoryData = {
    remoteService.getFullHistory match {
      case Success(stock) => parse(stock).extract[HistoryData]
      case Failure(e) => sys.error(e.getLocalizedMessage)
    }
  }

  def getCurrentStock: StockData = {
    remoteService.getCurrentStockPrice match {
      case Success(stock) => parse(stock).extract[StockData]
      case Failure(e) => sys.error(e.getLocalizedMessage)
    }
  }
}
