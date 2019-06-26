package pl.suu.predictor.spark.model

case class SentimentTweet(id: Long, sentimentBayes: Int, sentimentNLP: Int, text: String)
