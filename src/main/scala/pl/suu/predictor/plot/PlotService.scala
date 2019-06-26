package pl.suu.predictor.plot

import vegas._
import vegas.data.External._
import vegas.DSL.SpecBuilder
import vegas.{Field, Legend, Line, Nominal, Quant, Temp, Vegas}

case class PlotService(width: Double, height: Double) {
  def plot(data: Seq[Map[String, Any]]): Unit = {
    val plot = Vegas("Sample Multi Series Line Chart", width = Some(width), height = Some(height))
      .withData(data)
      .mark(Bar)
      .encodeY("value", Quant, aggregate = AggOps.Mean)
      .encodeColor(
        field = "symbol",
        dataType = Nominal,
        legend = Legend(orient = "left", title = "Stock Symbol"))
      .encodeDetailFields(Field(field = "symbol", dataType = Nominal)
      )
    plot.show
  }
}
