package pl.suu.predictor.stock

import org.joda.time.{DateTime, Days, Duration, Hours, Period}
import org.joda.time.format.{DateTimeFormat, PeriodFormatter}

case class StockProcessor(stockService: StockService) {
  def process: List[Map[String, Any]] = {
    stockService.getHoursCSV
      .map {
        csv =>
          DateTime.parse(
            csv.date, DateTimeFormat.forPattern("yyyy-MM-dd"))
            .plus(Duration.millis(DateTime.parse(csv.time, DateTimeFormat.forPattern("HH:mm:ss")).getMillis)) -> csv.open.toDouble
      }
      .filter{
        case (date, _) => date.isAfter(new org.joda.time.DateTime().minus(Days.days(8)))
      }
      .map {
        case (date, value) => date.toString("yyyy-MM-dd'T'HH:mm:ss") -> value
      }
      .groupBy(xs => xs._1)
      .map {
        case (date, values) => Map("symbol" -> stockService.stock, "date" -> date, "value" -> values.map(_._2).sum / values.size)
      }.toList
  }
}
