name := "mtspredbot"
scalaVersion := "2.12.4"
version := "1.0"

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.3.0-alpha4",
  "org.scala-lang" % "scala-library" % "2.12.4"
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "plugin.properties" => MergeStrategy.last
  case "log4j.properties" => MergeStrategy.last
  case "logback.xml" => MergeStrategy.last
  case "resources/logback.xml" => MergeStrategy.last
  case "resources/application.conf" => MergeStrategy.last
  case "application.conf" => MergeStrategy.last
  case PathList("reference.conf") => MergeStrategy.concat
  case x => MergeStrategy.first
}

assemblyJarName in assembly :="mtspredbot.jar"
mainClass in (Compile, packageBin) := Some("mtspredbot.Main")
mainClass in (Compile, run) := Some("mtspredbot.Main")
