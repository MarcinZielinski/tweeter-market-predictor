package pl.suu.predictor.stock
import pl.suu.predictor.stock.model.StockCsvData
import resource.managed

import scala.io.Source
import scala.util.Try

case class LocalDataProvider(stock: String = "kghm") {
  def getHoursCsv: Try[List[StockCsvData]] = {
    (for {
      result <- managed(Source.fromURL(getClass.getResource(s"$stock-minutes.csv")))
    } yield result.getLines().drop(1).map(line => StockCsvData.from(line.split(","))).toList).tried
  }
}
