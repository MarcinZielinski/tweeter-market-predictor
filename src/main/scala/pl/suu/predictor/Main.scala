package pl.suu.predictor

import com.typesafe.config.ConfigFactory
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.spark.streaming.twitter.TwitterUtils
import twitter4j.auth.OAuthAuthorization
import twitter4j.conf.ConfigurationBuilder

// Create a local StreamingContext with two working thread and batch interval of 5 second.
// The master requires 2 cores to prevent from a starvation scenario.
object Main extends App {
  val conf = new SparkConf().setMaster("local[2]").setAppName("TwitterData")
  val ssc = new StreamingContext(conf, Seconds(60))

  val config = ConfigFactory.load()
  val accessToken = config.getString("ACCESS_TOKEN_KEY")
  val accessTokenSecret = config.getString("ACCESS_TOKEN_SECRET")
  val consumerKey = config.getString("CONSUMER_KEY")
  val consumerSecret = config.getString("CONSUMER_SECRET")
  val filters = Seq("KGHM","cuprum", "Chludzinski", "Chludziński", "Chludzienskiemu", "Chludzińskiego")
  val cb = new ConfigurationBuilder
  cb.setDebugEnabled(true).setOAuthConsumerKey(consumerKey)
    .setOAuthConsumerSecret(consumerSecret)
    .setOAuthAccessToken(accessToken)
    .setOAuthAccessTokenSecret(accessTokenSecret)
  val auth = new OAuthAuthorization(cb.build)
//  val tf = new TwitterFactory(cb.build())
//  val twitter = tf.getInstance()
//  val date = new Date().toInstant.minus(7, ChronoUnit.DAYS).toString
//  val tweets = twitter.search(new Query().lang("en").since(date))

  val tweets = TwitterUtils.createStream(ssc, Some(auth), filters).filter(_.getLang == "en")
  tweets.saveAsTextFiles("tweets", "json")

  //  // Split each line into words
  //  val words = lines.flatMap(_.split(" "))
  //
  //  // Count each word in each batch
  //  val pairs = words.map(word => (word, 1))
  //  val wordCounts = pairs.reduceByKey(_ + _)
  //
  //  // Print the first ten elements of each RDD generated in this DStream to the console
  //  wordCounts.print()

  ssc.start()
  ssc.awaitTermination()

//  SparkNaiveBayesModelCreator.main(Array.empty)
//  TweetSentimentAnalyzer.main(Array.empty)
//  def getFromKeywordSingleDay(i: DateTime,k: String, count: Int): String = {
//    val consumer = new CommonsHttpOAuthConsumer(ConsumerKey, ConsumerSecret)
//    consumer.setTokenWithSecret(AccessToken, AccessSecret)
//    val url = "https://api.twitter.com/1.1/search/tweets.json?q=" + k + "&count=" + count + "&until=" + i.toString(StaticDateTimeFormat.forPattern("yyyy-MM-dd"))
//    //println(url)
//    val request = new HttpGet(url)
//    consumer.sign(request)
//    val client = HttpClientBuilder.create().build()
//    val response = client.execute(request)
//    IOUtils.toString(response.getEntity().getContent())
//  }
//
//
//  def getFromKeyword(k: String, count: Int = 90): String = {
//    val today= DateTime.now
//    //println(today.toString(StaticDateTimeFormat.forPattern("yyyy-MM-dd")))
//    val ss = for (i <- 1 to 7) yield getFromKeywordSingleDay(today-i.days,k,count)
//    ss.mkString("\n")
//  }
}
