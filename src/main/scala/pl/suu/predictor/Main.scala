package pl.suu.predictor

import com.typesafe.config.ConfigFactory
import org.apache.spark._
import org.apache.spark.mllib.classification.NaiveBayesModel
import org.apache.spark.streaming._
import org.joda.time.DateTime
import org.slf4j.{Logger, LoggerFactory}
import pl.suu.predictor.csv.CsvWriter
import pl.suu.predictor.sentiment.corenlp.CoreNLPSentimentAnalyzer.computeSentiment
import pl.suu.predictor.sentiment.mllib.MLlibSentimentAnalyzer
import pl.suu.predictor.sentiment.utils.{PropertiesLoader, StopwordsLoader}
import pl.suu.predictor.spark.TwitterReceiver
import pl.suu.predictor.spark.model.{CsvTweet, CsvTweetWithText, SentimentTweet, ToSeqable}
import twitter4j.{Status, TwitterFactory}

import scala.collection.mutable.ListBuffer

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
      SentimentTweet(tweet.getId, getSentiment(tweet), computeSentiment(tweet.getText), s"${tweet.getText} #${tweet.getHashtagEntities.mkString(" #")}")))
    .groupByKey
    .foreachRDD(tweet => {
      if (tweet.count > 0) {
        processedTweets ++= tweet.collect
      }
    })

  startSpark

  val csvTweets: List[ToSeqable] = extractAggregatedSentiment

  CsvWriter.save("twitter-sentiment-with-text.csv", csvTweets)

  private def extractTweetsTextWithSentiments = {
    processedTweets
      .distinct
      .flatMap {
        case (date, sentimentTweets) => sentimentTweets
          .map(tweet => CsvTweetWithText(tweet.text, translateSentimentToString(tweet.sentimentBayes), translateSentimentToString(tweet.sentimentNLP)))
      }
  }

  def translateSentimentToString(sentimentValue: Int): String = {
    sentimentValue match {
      case -1 => "Negative"
      case 0 => "Neutral"
      case 1 => "Positive"
    }
  }

  private def extractSentimentsWithDates = {
    processedTweets
      .distinct
      .map {
        case (date, sentiments) =>
          val positiveTweets = sentiments.count(_.sentimentBayes == 1)
          val neutralTweets = sentiments.count(_.sentimentBayes == 0)
          val negativeTweets = sentiments.count(_.sentimentBayes == -1)

          val positiveTweetsNlp = sentiments.count(_.sentimentNLP == 1)
          val neutralTweetsNlp = sentiments.count(_.sentimentNLP == 0)
          val negativeTweetsNlp = sentiments.count(_.sentimentNLP == -1)

          CsvTweet(date, positiveTweets, neutralTweets, negativeTweets, positiveTweetsNlp, neutralTweetsNlp, negativeTweetsNlp)
      }
      .sortBy(_.date)
  }

  private def extractAggregatedSentiment = {
    val dateSenti: List[CsvTweet] = processedTweets
      .distinct
      .map {
        case (date, sentiments) =>
          val positiveTweets = sentiments.count(_.sentimentBayes == 1)
          val neutralTweets = sentiments.count(_.sentimentBayes == 0)
          val negativeTweets = sentiments.count(_.sentimentBayes == -1)

          val positiveTweetsNlp = sentiments.count(_.sentimentNLP == 1)
          val neutralTweetsNlp = sentiments.count(_.sentimentNLP == 0)
          val negativeTweetsNlp = sentiments.count(_.sentimentNLP == -1)

          CsvTweet(date, positiveTweets, neutralTweets, negativeTweets, positiveTweetsNlp, neutralTweetsNlp, negativeTweetsNlp)
      }
      .sortBy(_.date)


    var previousTweet: CsvTweet = null
    var listBuffer: ListBuffer[CsvTweet] = ListBuffer()
    for (i <- 1 until dateSenti.length) {
      val csvTweet = dateSenti(i)
      val csvTweetDateTime = new DateTime(csvTweet.date.replace(' ', 'T'))
      val hour = csvTweetDateTime.getHourOfDay
      val dayOfTheWeek = csvTweetDateTime.getDayOfWeek

      if (hour > 17 || hour < 10 || dayOfTheWeek == 7 || dayOfTheWeek == 6 || csvTweetDateTime.getDayOfMonth == 20) {
        if (previousTweet != null) {
          listBuffer -= previousTweet
          previousTweet = previousTweet.copy(
            positiveBayes = previousTweet.positiveBayes + csvTweet.positiveBayes,
            neutralBayes = previousTweet.neutralBayes + csvTweet.neutralBayes,
            negativeBayes = previousTweet.negativeBayes + csvTweet.negativeBayes,
            positiveNlp = previousTweet.positiveNlp + csvTweet.positiveNlp,
            neutralNlp = previousTweet.neutralNlp + csvTweet.neutralNlp,
            negativeNlp = previousTweet.negativeNlp + csvTweet.negativeNlp
          )
          listBuffer += previousTweet
        }
      } else {
        listBuffer += csvTweet
        previousTweet = csvTweet
      }
    }

    val bufferedTweets = listBuffer.toList

    var nowDate = DateTime.now()
    var nextDate = nowDate.minusDays(9)
    nowDate = new DateTime(nowDate.getYear, nowDate.getMonthOfYear, nowDate.getDayOfMonth, nowDate.getHourOfDay, 0, 0)
    nextDate = new DateTime(nextDate.getYear, nextDate.getMonthOfYear, nextDate.getDayOfMonth, 0, 0, 0)

    var allDatasBuffer: ListBuffer[CsvTweet] = ListBuffer()
    while(nextDate.isBefore(nowDate)) {
      val hours = nextDate.getHourOfDay
      val dayOfTheWeek = nextDate.getDayOfWeek
      if(hours <= 17 && hours >= 10 && dayOfTheWeek != 7 && dayOfTheWeek != 6 && nextDate.getDayOfMonth != 20) { // święto, giełda zamknięta
        val maybeTweet = bufferedTweets.find(_.date == nextDate.toString("yyyy-MM-dd HH':00:00'"))

        allDatasBuffer += maybeTweet.getOrElse(CsvTweet(nextDate.toString("yyyy-MM-dd HH':00:00'"), 0, 0, 0, 0, 0, 0))
      }

      nextDate = nextDate.plusHours(1)
    }

    allDatasBuffer.toList
  }

  private def startSpark = {
    ssc.start()
    ssc.awaitTerminationOrTimeout(100000)
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
