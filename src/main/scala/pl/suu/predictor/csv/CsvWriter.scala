package pl.suu.predictor.csv

import java.io.File

import com.github.tototoshi.csv.CSVWriter
import pl.suu.predictor.spark.model.ToSeqable

case object CsvWriter {
  def save(filename: String, data: Seq[ToSeqable]): Unit = {

    val file = new File(s"src/main/resources/$filename")

    val writer = CSVWriter.open(file)

    data.headOption.foreach(head => writer.writeRow(head.header))

    data
      .map(_.toSeq)
      .foreach(csvRow => {
        writer.writeRow(csvRow)
      })

    writer.close()
  }
}
