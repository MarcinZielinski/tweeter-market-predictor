package pl.suu.predictor.sentiment

import java.text.SimpleDateFormat

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.hadoop.io.compress.GzipCodec
import org.apache.spark.SparkConf
import org.apache.spark.mllib.classification.NaiveBayesModel
import org.apache.spark.rdd.RDD
import org.apache.spark.serializer.KryoSerializer
import org.apache.spark.sql.SaveMode
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Durations, StreamingContext}
import pl.suu.predictor.sentiment.mllib.MLlibSentimentAnalyzer
import pl.suu.predictor.sentiment.utils._
import twitter4j.Status
import twitter4j.auth.OAuthAuthorization

/**
  * Analyzes and predicts Twitter Sentiment in [near] real-time using Spark Streaming and Spark MLlib.
  * Uses the Naive Bayes Model created from the Training data and applies it to predict the sentiment of tweets
  * collected in real-time with Spark Streaming, whose batch is set to 20 seconds [configurable].
  * Raw tweets [compressed] and also the gist of predicted tweets are saved to the disk.
  * At the end of the batch, the gist of predicted tweets is published to Redis.
  * Any frontend app can subscribe to this Redis Channel for data visualization.
  */
// spark-submit --class "org.p7h.spark.sentiment.TweetSentimentAnalyzer" --master spark://spark:7077 mllib-tweet-sentiment-analysis-assembly-0.1.jar
object TweetSentimentAnalyzer {

  def main(args: Array[String]): Unit = {
    val ssc = StreamingContext.getActiveOrCreate(createSparkStreamingContext)
    val simpleDateFormat = new SimpleDateFormat("EE MMM dd HH:mm:ss ZZ yyyy")

    LogUtils.setLogLevels(ssc.sparkContext)

    // Load Naive Bayes Model from the location specified in the config file.
    val naiveBayesModel = NaiveBayesModel.load(ssc.sparkContext, PropertiesLoader.naiveBayesModelPath)
    val stopWordsList = ssc.sparkContext.broadcast(StopwordsLoader.loadStopWords(PropertiesLoader.nltkStopWords))

    /**
      * Predicts the sentiment of the tweet passed.
      * Invokes Stanford Core NLP and MLlib methods for identifying the tweet sentiment.
      *
      * @param status -- twitter4j.Status object.
      * @return tuple with Tweet ID, Tweet Text, Core NLP Polarity, MLlib Polarity, Latitude, Longitude, Profile Image URL, Tweet Date.
      */
    def predictSentiment(status: Status): (Long, String, String, Int, Double, Double, String, String) = {
      val tweetText = replaceNewLines(status.getText)
      val mllibSentiment =
        if (isTweetInEnglish(status)) MLlibSentimentAnalyzer.computeSentiment(tweetText, stopWordsList, naiveBayesModel) else 0

      (status.getId,
        status.getUser.getScreenName,
        tweetText,
        mllibSentiment,
        status.getGeoLocation.getLatitude,
        status.getGeoLocation.getLongitude,
        status.getUser.getOriginalProfileImageURL,
        simpleDateFormat.format(status.getCreatedAt))
    }

    val oAuth: Some[OAuthAuthorization] = OAuthUtils.bootstrapTwitterOAuth()
    val rawTweets = TwitterUtils.createStream(ssc, oAuth)

    // Save Raw tweets only if the flag is set to true.
    if (PropertiesLoader.saveRawTweets) {
      rawTweets.cache()

      rawTweets.foreachRDD { rdd =>
        if (rdd != null && !rdd.isEmpty() && !rdd.partitions.isEmpty) {
          saveRawTweetsInJSONFormat(rdd, PropertiesLoader.tweetsRawPath)
        }
      }
    }

    // This delimiter was chosen as the probability of this character appearing in tweets is very less.
    val DELIMITER = "¦"
    val tweetsClassifiedPath = PropertiesLoader.tweetsClassifiedPath
    val filteredTweets = rawTweets.filter(hasGeoLocation)
    val classifiedTweets = filteredTweets.map(predictSentiment)

    ssc.start()
    ssc.awaitTerminationOrTimeout(PropertiesLoader.totalRunTimeInMinutes * 60 * 1000) // auto-kill after processing rawTweets for n mins.
  }

  /**
    * Create StreamingContext.
    * Future extension: enable checkpointing to HDFS [is it really required??].
    *
    * @return StreamingContext
    */
  def createSparkStreamingContext(): StreamingContext = {
    val conf = new SparkConf().setMaster("local[2]")
      .setAppName(this.getClass.getSimpleName)
      // Use KryoSerializer for serializing objects as JavaSerializer is too slow.
      .set("spark.serializer", classOf[KryoSerializer].getCanonicalName)
      // For reconstructing the Web UI after the application has finished.
      .set("spark.eventLog.enabled", "true")
      // Reduce the RDD memory usage of Spark and improving GC behavior.
      .set("spark.streaming.unpersist", "true")

    val ssc = new StreamingContext(conf, Durations.seconds(PropertiesLoader.microBatchTimeInSeconds))
    ssc
  }

  /**
    * Jackson Object Mapper for mapping twitter4j.Status object to a String for saving raw tweet.
    */
  val jacksonObjectMapper: ObjectMapper = new ObjectMapper()

  /**
    * Saves raw tweets received from Twitter Streaming API in
    *
    * @param rdd           -- RDD of Status objects to save.
    * @param tweetsRawPath -- Path of the folder where raw tweets are saved.
    */
  def saveRawTweetsInJSONFormat(rdd: RDD[Status], tweetsRawPath: String): Unit = {
    val sqlContext = SQLContextSingleton.getInstance(rdd.sparkContext)
    val tweet = rdd.map(status => jacksonObjectMapper.writeValueAsString(status))
    val rawTweetsDF = sqlContext.read.json(tweet)
    rawTweetsDF.coalesce(1).write
      .format("org.apache.spark.sql.json")
      // Compression codec to compress when saving to file.
      .option("codec", classOf[GzipCodec].getCanonicalName)
      .mode(SaveMode.Append)
      .save(tweetsRawPath)
  }

  /**
    * Removes all new lines from the text passed.
    *
    * @param tweetText -- Complete text of a tweet.
    * @return String without new lines.
    */
  def replaceNewLines(tweetText: String): String = {
    tweetText.replaceAll("\n", "")
  }

  /**
    * Checks if the tweet Status is in English language.
    * Actually uses profile's language as well as the Twitter ML predicted language to be sure that this tweet is
    * indeed English.
    *
    * @param status twitter4j Status object
    * @return Boolean status of tweet in English or not.
    */
  def isTweetInEnglish(status: Status): Boolean = {
    status.getLang == "en" && status.getUser.getLang == "en"
  }

  /**
    * Checks if the tweet Status has Geo-Coordinates.
    *
    * @param status twitter4j Status object
    * @return Boolean status of presence of Geolocation of the tweet.
    */
  def hasGeoLocation(status: Status): Boolean = {
    null != status.getGeoLocation
  }
}