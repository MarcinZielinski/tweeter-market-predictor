package pl.suu.predictor.stock


import java.text.DateFormat

import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import resource._

import scala.io.Source
import scala.util.Success

class StockDataServiceSpec extends WordSpec with Matchers with MockFactory {
  "StockService" should {

    "handle intraday get HTTP request correctly" in {
      val remoteService = stub[RemoteDataProvider]


      for (response <- managed(StockDataServiceSpec.intradayResponse)) {
        (remoteService.getIntradayStockPrice _).when(7, 1).returns(Success(response.mkString))
      }

      val intradayData = StockDataService(remoteService).getStockFromLast7Days.get

      intradayData.symbol shouldBe "KGHA.F"
      intradayData.intraday("2019-06-07 14:35:00").high shouldBe "22.00"
    }


    "handle current stock get HTTP request correctly" in {
      val remoteService = stub[RemoteDataProvider]


      for (response <- managed(StockDataServiceSpec.currentStockResponse)) {
        (remoteService.getCurrentStockPrice _).when().returns(Success(response.mkString))
      }

      val currentStockData = StockDataService(remoteService).getCurrentStock.get

      currentStockData.data.head.symbol shouldBe "KGHA.F"
      currentStockData.data.head.price shouldBe "22.00"
    }


    "handle full history get HTTP request correctly" in {
      val remoteService = stub[RemoteDataProvider]


      for (response <- managed(StockDataServiceSpec.fullHistoryResponse)) {
        (remoteService.getFullHistory _).when().returns(Success(response.mkString))
      }

      val historyData = StockDataService(remoteService).getFullHistory.get

      historyData.name shouldBe "KGHA.F"
      historyData.history("2019-06-07").high shouldBe "22.00"
    }
  }
}

object StockDataServiceSpec {
  val intradayResponse: Source = Source.fromURL(getClass.getResource("kghm-last-7-days.json"))
  val currentStockResponse: Source = Source.fromURL(getClass.getResource("kghm-current.json"))
  val fullHistoryResponse: Source = Source.fromURL(getClass.getResource("kghm-history.json"))
}