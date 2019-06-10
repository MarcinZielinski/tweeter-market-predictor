package pl.suu.predictor

import com.typesafe.config.ConfigFactory
import org.apache.spark._
import org.apache.spark.mllib.classification.NaiveBayesModel
import org.apache.spark.streaming._
import org.slf4j.{Logger, LoggerFactory}
import pl.suu.predictor.sentiment.mllib.MLlibSentimentAnalyzer
import pl.suu.predictor.sentiment.utils.{PropertiesLoader, StopwordsLoader}
import pl.suu.predictor.spark.TwitterReceiver
import pl.suu.predictor.stock.{LocalDataProvider, StockService}
import twitter4j.{Status, TwitterFactory}
import vegas._

// Create a local StreamingContext with two working thread and batch interval of 1 second.
// The master requires 2 cores to prevent from a starvation scenario.
object Main extends App {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  val conf = new SparkConf().setMaster("local[2]").setAppName("TwitterData")
  val ssc = new StreamingContext(conf, Seconds(1))
  val naiveBayesModel = NaiveBayesModel.load(ssc.sparkContext, PropertiesLoader.naiveBayesModelPath)
  val stopWordsList = ssc.sparkContext.broadcast(StopwordsLoader.loadStopWords(PropertiesLoader.nltkStopWords))

  val config = ConfigFactory.load()
  val filters = Seq("KGHM", "cuprum", "Chludzinski", "Chludziński", "Chludzienskiemu", "Chludzińskiego")
  val tf = new TwitterFactory()
  val twitter = tf.getInstance()
  val stream = ssc.receiverStream(new TwitterReceiver(twitter))

  var list: List[Map[String, Any]] = List()


  stream
    .map(tweet => (new org.joda.time.DateTime(tweet.getCreatedAt).toString("yyyy-MM-dd"), getSentiment(tweet)))
    .groupByKey()
    .map {
      case (date, sentimentArray) => Map("symbol" -> "tweet", "date" -> date, "sentiment" -> sentimentArray.sum)
    }
    .foreachRDD(tweet => {
      if(tweet.count > 0) {
        list ++= tweet.collect
      }
    })

  ssc.start()
  ssc.awaitTerminationOrTimeout(5000)
//  ssc.awaitTermination()

//  logger.info(tweetMap("tweet")(0)._1)


  val stockService = StockService(LocalDataProvider())

  val stock = stockService.getStockFromLast7Days.intraday.map {
    case (date, values) => org.joda.time.DateTime.parse(
      date, org.joda.time.format.DateTimeFormat.forPattern("yyyy-MM-ddd HH:mm:ss"))
      .toString("yyyy-MM-dd") -> values
  }
    .groupBy(xs => xs._1)
    .map {
      case (date, values) => Map("symbol" -> "KGHM", "date" -> date, "sentiment" -> values.map(_._2.close.toDouble).sum)
    }

  list ++= stock

  val plot = Vegas("Sample Multi Series Line Chart", width = Some(800.0), height = Some(600.0))

    /*
    symbol	date	price
    MSFT	Jan 1 2000	39.81
     */
    .withData(list)
    .mark(Line)
    .encodeX("date", Temp)
    .encodeY("sentiment", Quant)
    .encodeColor(
      field = "symbol",
      dataType = Nominal,
      legend = Legend(orient = "left", title = "Stock Symbol"))
    .encodeDetailFields(Field(field = "symbol", dataType = Nominal))

  plot.show

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
