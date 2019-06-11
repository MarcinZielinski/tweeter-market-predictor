package pl.suu.predictor.stock

import java.io.File
import java.nio.file.Paths
import java.io._

import net.liftweb.json._
import net.liftweb.json.{DefaultFormats, Extraction, JsonAST}
import pl.suu.predictor.stock.model.{HistoryData, IntradayData}

case class StockDataPersister(stock: String) {

  implicit val format: DefaultFormats = DefaultFormats

  def persist(data: HistoryData): Unit = {
    val res = getClass.getResource("")
    val file = Paths.get(res.toURI).toFile
    val absolutePath = file.getAbsolutePath
    printToFile(new File(s"$absolutePath/$stock-history.json"))(p => p.println(prettyRender(Extraction.decompose(data))))
  }


  def persist(data: IntradayData): Unit = {
    val res = getClass.getResource("")
    val file = Paths.get(res.toURI).toFile
    val absolutePath = file.getAbsolutePath
    printToFile(new File(s"$absolutePath/$stock-last-7-days.json"))(p => p.println(prettyRender(Extraction.decompose(data))))
  }

  private def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }
}
