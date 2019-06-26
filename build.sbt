name := "twitter-market-predictor"

version := "0.1"

scalaVersion := "2.11.12"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalamock" %% "scalamock" % "4.1.0" % Test,
  "net.liftweb" %% "lift-json" % "3.3.0",
  "com.jsuereth" %% "scala-arm" % "2.0",
  "org.apache.spark" %% "spark-streaming" % "2.4.3",
  "org.apache.spark" %% "spark-core" % "2.4.3",
  "org.apache.spark" %% "spark-sql" % "2.4.3",
  "org.apache.spark" %% "spark-mllib" % "2.4.3",
  "org.apache.spark" %% "spark-streaming-twitter" % "1.6.3",
  "com.typesafe" % "config" % "1.3.4",
  "org.vegas-viz" %% "vegas" % "0.3.11",
  "org.vegas-viz" %% "vegas-spark" % "0.3.11",
  "com.github.tototoshi" %% "scala-csv" % "1.3.6",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.9.2",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.9.2" classifier "models",
  "com.github.fommil.netlib" % "all" % "1.1.2" pomOnly()
)
