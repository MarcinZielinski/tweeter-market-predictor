package pl.suu.predictor

import com.typesafe.config.ConfigFactory
import org.apache.spark._
import org.apache.spark.mllib.classification.NaiveBayesModel
import org.apache.spark.streaming._
import org.joda.time.Days
import org.slf4j.{Logger, LoggerFactory}
import pl.suu.predictor.plot.PlotService
import pl.suu.predictor.sentiment.mllib.MLlibSentimentAnalyzer
import pl.suu.predictor.sentiment.utils.{PropertiesLoader, StopwordsLoader}
import pl.suu.predictor.spark.TwitterReceiver
import pl.suu.predictor.stock.{LocalDataProvider, StockDataService, StockProcessor, StockService}
import twitter4j.{Status, TwitterFactory}
import vegas._

// Create a local StreamingContext with two working thread and batch interval of 1 second.
// The master requires 2 cores to prevent from a starvation scenario.
object Main extends App {

  val stockName = args.headOption.getOrElse("KGHA.F")
  val filters = if (args.drop(1).isEmpty) List("KGHM", "cuprum", "Chludzinski", "Chludziński") else args.drop(1).toList

  val logger: Logger = LoggerFactory.getLogger(getClass)

  val conf = new SparkConf().setMaster("local[2]").setAppName("TwitterData")
  val ssc = new StreamingContext(conf, Seconds(1))

  val naiveBayesModel = NaiveBayesModel.load(ssc.sparkContext, PropertiesLoader.naiveBayesModelPath)
  val stopWordsList = ssc.sparkContext.broadcast(StopwordsLoader.loadStopWords(PropertiesLoader.nltkStopWords))

  val config = ConfigFactory.load()
  val tf = new TwitterFactory()
  val twitter = tf.getInstance()
  val stream = ssc.receiverStream(new TwitterReceiver(twitter, filters))


  var list: List[Map[String, Any]] = List()
  stream
    .map(tweet => (new org.joda.time.DateTime(tweet.getCreatedAt).toString("yyyy-MM-dd"),
      getSentiment(tweet)))
    .groupByKey()


    .map {
      case (date, sentimentArray) => Map("symbol" -> "tweet", "date" -> date, "value" -> sentimentArray.sum)
    }
    .foreachRDD(tweet => {
      if(tweet.count > 0) {
        list ++= tweet.collect
      }
    })

  startSpark


  list ++= StockProcessor(StockService(stockName)).process

  PlotService(400.0, 400.0, list).plot


  private def startSpark = {
    ssc.start()
    ssc.awaitTerminationOrTimeout(5000)
  }

  def getSentiment(tweet: Status): Int = {
    val tweetText = replaceNewLines(tweet.getText)
    val mllibSentiment =
      if (isTweetInEnglish(tweet)) MLlibSentimentAnalyzer.computeSentiment(tweetText, stopWordsList, naiveBayesModel) else 0

    mllibSentiment
  }

  def isTweetInEnglish(status: Status): Boolean = {
    status.getLang == "en" && status.getUser.getLang == "en"
  }

  def replaceNewLines(tweetText: String): String = {
    tweetText.replaceAll("\n", "")
  }
}
