name := """flickr-exporter"""

version := "1.0"

scalaVersion := "2.11.4"

resolvers += Resolver.sonatypeRepo("public")

libraryDependencies ++= Seq(
  "com.flickr4java" % "flickr4java" % "2.12",
  "com.github.scopt" %% "scopt" % "3.2.0")

// Change this to another test framework if you prefer
libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.6" % "test"




// Uncomment to use Akka
//libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.3.3"

