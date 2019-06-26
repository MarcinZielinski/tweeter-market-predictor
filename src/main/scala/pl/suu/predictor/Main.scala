package pl.suu.predictor

import com.typesafe.config.ConfigFactory
import org.apache.spark._
import org.apache.spark.mllib.classification.NaiveBayesModel
import org.apache.spark.streaming._
import org.slf4j.{Logger, LoggerFactory}
import pl.suu.predictor.csv.CsvWriter
import pl.suu.predictor.sentiment.mllib.MLlibSentimentAnalyzer
import pl.suu.predictor.sentiment.utils.{PropertiesLoader, StopwordsLoader}
import pl.suu.predictor.spark.TwitterReceiver
import pl.suu.predictor.spark.model.{CsvTweet, CsvTweetWithText, SentimentTweet, ToSeqable}
import twitter4j.{Status, TwitterFactory}

// Create a local StreamingContext with two working thread and batch interval of 1 second.
// The master requires 2 cores to prevent from a starvation scenario.
object Main extends App {


  val stockName = args.headOption.getOrElse("kghm")
  val filters = if (args.drop(1).isEmpty) List("KGHM", "cuprum") else args.drop(1).toList

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

  type Date = String

  var processedTweets: List[(Date, Iterable[SentimentTweet])] = List()

  stream
    .map(tweet => (new org.joda.time.DateTime(tweet.getCreatedAt).toString("yyyy-MM-dd HH':00:00'"),
      SentimentTweet(tweet.getId, getSentiment(tweet), s"${tweet.getText} #${tweet.getHashtagEntities.mkString(" #")}")))
    .groupByKey
    .foreachRDD(tweet => {
      if (tweet.count > 0) {
        processedTweets ++= tweet.collect
      }
    })

  startSpark

  val csvTweets: List[ToSeqable] = extractTweetsTextWithSentiments

  CsvWriter.save("twitter-sentiment-with-text.csv", csvTweets)

  private def extractTweetsTextWithSentiments = {
    processedTweets
      .distinct
      .flatMap {
        case (date, sentimentTweets) => sentimentTweets.map(tweet => CsvTweetWithText(tweet.text, tweet.sentiment match {
          case -1 => "Negative"
          case 0 => "Positive"
          case 1 => "Negative"
        }))
      }
  }

  private def extractSentimentsWithDates = {
    processedTweets
      .distinct
      .map {
        case (date, sentimentTweets) => date -> sentimentTweets.map(_.sentiment)
      }
      .map {
        case (date, sentiments) =>
          val positiveTweets = sentiments.count(_ == 1)
          val neutralTweets = sentiments.count(_ == 0)
          val negativeTweets = sentiments.count(_ == -1)
          CsvTweet(date, positiveTweets, neutralTweets, negativeTweets)
      }
      .sortBy(_.date)
  }

  private def startSpark = {
    ssc.start()
    ssc.awaitTerminationOrTimeout(10000)
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
