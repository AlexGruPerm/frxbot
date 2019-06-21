name := "mtspredbot"
scalaVersion := "2.12.4"
version := "1.0"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scala-lang" % "scala-library" % "2.12.4",
  "com.typesafe" % "config" % "1.3.4",
   "com.bot4s" %% "telegram-core" % "4.2.0-RC1",
  "com.datastax.oss" % "java-driver-core" % "4.0.1"
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
