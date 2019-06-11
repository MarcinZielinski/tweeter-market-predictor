package pl.suu.predictor.stock

import pl.suu.predictor.stock.model.{HistoryData, IntradayData}

import scala.util.{Failure, Success}

case class StockService(stock: String = "KGHA.F") {
  val localDataService = StockDataService(LocalDataProvider(stock))
  val remoteDataService = StockDataService(RemoteDataProvider(stock))
  val stockDataPersister = StockDataPersister(stock)

  def getFullHistory: HistoryData = {
    localDataService.getFullHistory match {
      case Success(data) => data
      case Failure(_) =>
        remoteDataService.getFullHistory match {
          case Success(data) =>
            stockDataPersister.persist(data)
            data
          case Failure(e) => throw e
        }

    }
  }

  def getLast7Days: IntradayData = {
    localDataService.getStockFromLast7Days match {
      case Success(data) => data
      case Failure(_) =>
        remoteDataService.getStockFromLast7Days match {
          case Success(data) =>
            stockDataPersister.persist(data)
            data
          case Failure(e) => throw e
        }

    }
  }
}
