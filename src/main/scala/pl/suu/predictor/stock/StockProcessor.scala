package pl.suu.predictor.stock

import org.joda.time.Days

case class StockProcessor(stockService: StockService) {
  def process: List[Map[String, Any]] = {
    stockService.getLast7Days.intraday
      .map {
        case (date, values) => org.joda.time.DateTime.parse(
          date, org.joda.time.format.DateTimeFormat.forPattern("yyyy-MM-ddd HH:mm:ss")) -> values
      }
      .filter{
        case (date, values) => date.isAfter(new org.joda.time.DateTime().minus(Days.days(8)))
      }
      .map {
        case (date, values) => date.toString("yyyy-MM-dd") -> values
      }
      .groupBy(xs => xs._1)
      .map {
        case (date, values) => Map("symbol" -> stockService.stock, "date" -> date, "value" -> values.map(_._2.close.toDouble).sum / values.size)
      }.toList
  }
}
