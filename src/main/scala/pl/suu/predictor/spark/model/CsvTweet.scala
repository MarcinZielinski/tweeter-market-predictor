package pl.suu.predictor.spark.model

sealed trait ToSeqable {
  val header: Seq[String]

  def toSeq: Seq[_]
}

case class CsvTweet(date: String, positiveBayes: Int, neutralBayes: Int, negativeBayes: Int, positiveNlp: Int,
                    neutralNlp: Int, negativeNlp: Int) extends ToSeqable {
  override def toSeq: Seq[_] = Seq(date, positiveBayes, neutralBayes, negativeBayes, positiveNlp, neutralNlp, negativeNlp)

  override val header: Seq[String] = Seq("date", "positiveBayes", "neutralBayes", "negativeBayes", "positiveNlp", "neutralNlp", "negativeNlp")
}

case class CsvTweetWithText(tweet: String, attitudeBayes: String, attitudeNLP: String) extends ToSeqable {
  override def toSeq: Seq[_] = Seq(tweet, attitudeBayes, attitudeNLP)

  override val header: Seq[String] = Seq("tweet", "attitudeBayes", "attitudeNLP")
}
