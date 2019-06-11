package pl.suu.predictor.plot

import vegas.{Field, Legend, Line, Nominal, Quant, Temp, Vegas}

case class PlotService(width: Double, height: Double, data: List[Map[String, Any]]) {
  def plot: Unit = {
    val plot = Vegas("Sample Multi Series Line Chart", width = Some(400.0), height = Some(400.0))
      .withData(data)
      .mark(Line)
      .encodeX("date", Temp)
      .encodeY("value", Quant)
      .encodeColor(
        field = "symbol",
        dataType = Nominal,
        legend = Legend(orient = "left", title = "Stock Symbol"))
      .encodeDetailFields(Field(field = "symbol", dataType = Nominal))

    plot.show
  }
}
