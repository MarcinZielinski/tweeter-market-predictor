package pl.suu.predictor

import com.typesafe.config.ConfigFactory
import org.apache.spark._
import org.apache.spark.streaming._
import pl.suu.predictor.spark.TwitterReceiver
import twitter4j.TwitterFactory

// Create a local StreamingContext with two working thread and batch interval of 1 second.
// The master requires 2 cores to prevent from a starvation scenario.
object Main extends App {
  val conf = new SparkConf().setMaster("local[2]").setAppName("TwitterData")
  val ssc = new StreamingContext(conf, Seconds(1))

  val config = ConfigFactory.load()
  val filters = Seq("KGHM", "cuprum", "Chludzinski", "Chludziński", "Chludzienskiemu", "Chludzińskiego")
  val tf = new TwitterFactory()
  val twitter = tf.getInstance()
  val stream = ssc.receiverStream(new TwitterReceiver(twitter))

  stream.map(tweet => s"${tweet.getCreatedAt}: ${tweet.getText}").print(20)

  ssc.start()
  ssc.awaitTermination()

  //  SparkNaiveBayesModelCreator.main(Array.empty)
  //  TweetSentimentAnalyzer.main(Array.empty)
}
