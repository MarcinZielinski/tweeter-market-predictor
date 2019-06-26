package pl.suu.predictor.csv

import java.io.File

import com.github.tototoshi.csv.CSVWriter
import pl.suu.predictor.spark.model.CsvTweet

case object CsvWriter {
  def save(filename: String, header: Seq[String], data: Seq[CsvTweet]): Unit = {

    val file = new File(s"src/main/resources/$filename")

    val writer = CSVWriter.open(file)

    writer.writeRow(header)

    data
      .map(tweet => Seq(tweet.date, tweet.positive, tweet.neutral, tweet.negative))
      .foreach(csvRow => {
        writer.writeRow(csvRow)
      })

    writer.close()
  }
}
