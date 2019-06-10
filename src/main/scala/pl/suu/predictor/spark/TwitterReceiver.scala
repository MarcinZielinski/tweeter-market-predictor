package pl.suu.predictor.spark

import java.util.concurrent.ForkJoinPool

import org.apache.spark.internal.Logging
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.receiver.Receiver
import org.joda.time.DateTime
import twitter4j.{Query, Status, Twitter}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class TwitterReceiver(twitter: Twitter)
  extends Receiver[Status](StorageLevel.MEMORY_AND_DISK_2) with Logging {


  def onStart() {
    new Thread("Twitter Receiver") {
      override def run() {
        receive()
      }
    }.start()
  }

  def onStop() {
    // There is nothing much to do as the thread calling receive() is designed to stop by itself
  }

  private def receive() {
    try {
      implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool(4))

      for {
        i <- 0 to 8
        dateUntil = DateTime.now().minusDays(i).toString("yyyy-MM-dd")
        dateSince = DateTime.now().minusDays(i + 1).toString("yyyy-MM-dd")
      } {
        for {
          tw <- Future(twitter.search(new Query("KGHM OR cuprum OR Chludzinski OR Chludziński").lang("en").since(dateSince).until(dateUntil).count(100)))
        } yield store(tw.getTweets.asScala.iterator)
      }
    } catch {
      case t: Throwable =>
        // restart if there is error
        restart("Error receiving data", t)
    }
  }
}
