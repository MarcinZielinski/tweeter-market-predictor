package pl.suu.predictor.spark.model

sealed trait ToSeqable {
  val header: Seq[String]
  def toSeq: Seq[_]
}

case class CsvTweet(date: String, positive: Int, neutral: Int, negative: Int) extends ToSeqable {
  override def toSeq: Seq[_] = Seq(date, positive, neutral, negative)

  override val header: Seq[String] = Seq("date", "positive", "neutral", "negative")
}

case class CsvTweetWithText(tweet: String, attitude: String) extends ToSeqable {
  override def toSeq: Seq[_] = Seq(tweet, attitude)

  override val header: Seq[String] = Seq("tweet", "attitude")
}
