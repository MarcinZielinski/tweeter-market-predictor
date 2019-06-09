name := "twitter-market-predictor"

version := "0.1"

scalaVersion := "2.12.0"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalamock" %% "scalamock" % "4.1.0" % Test,
  "net.liftweb" %% "lift-json" % "3.3.0",
  "com.jsuereth" %% "scala-arm" % "2.0",
"org.apache.spark" %% "spark-streaming" % "2.4.3",
  "org.apache.spark" %% "spark-core" % "2.4.3",
  "org.apache.spark" %% "spark-sql" % "2.4.3",
  "org.apache.spark" %% "spark-mllib" % "2.4.3",
  "org.apache.spark" % "spark-streaming-twitter_2.11" % "1.6.3",
  "com.typesafe" % "config" % "1.3.4"
)
