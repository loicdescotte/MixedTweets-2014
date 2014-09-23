name := "MixTweets"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  ws,
  "org.twitter4j" % "twitter4j-core" % "4.0.2",
  "org.twitter4j" % "twitter4j-async" % "4.0.2",
  "org.twitter4j" % "twitter4j-stream" % "4.0.2",
  "org.twitter4j" % "twitter4j-media-support" % "4.0.2"
)     

lazy val root = (project in file(".")).enablePlugins(PlayScala)
