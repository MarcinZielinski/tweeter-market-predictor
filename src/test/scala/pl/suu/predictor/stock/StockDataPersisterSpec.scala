package pl.suu.predictor.stock

import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import pl.suu.predictor.stock.StockDataServiceSpec.getClass
import pl.suu.predictor.stock.model.{HistoryData, StockValues}
import resource.managed
import net.liftweb.json._

import scala.io.Source
import scala.util.Success

class StockDataPersisterSpec extends WordSpec with Matchers with MockFactory {

  implicit val formats: DefaultFormats = DefaultFormats

  "StockDataPersister" should {
    "persist history data" in {
      val stockDataPersister = StockDataPersister("test")

      val historyData = HistoryData("test", Map("2019-06-07" -> StockValues("2.0", "3.0", "2.0", "1.2", "3.3")))

      stockDataPersister.persist(historyData)


      for (file <- managed(Source.fromURL(getClass.getResource("test-history.json")))) {
        val fileStr = file.mkString
        val extractedHistory = parse(fileStr).extract[HistoryData]
        extractedHistory.name shouldBe "test"
        extractedHistory.history("2019-06-07").volume shouldBe "3.3"
      }
    }
  }
}
