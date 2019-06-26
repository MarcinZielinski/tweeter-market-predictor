package pl.suu.predictor.stock

import pl.suu.predictor.stock.model.StockCsvData

import scala.util.{Failure, Success}

case class StockService(stock: String = "kghm") {
  val localDataService = LocalDataProvider(stock)

  def getHoursCSV: List[StockCsvData] = {
    localDataService.getHoursCsv match {
      case Success(data) => data
      case Failure(e) => throw e
    }
  }

}
