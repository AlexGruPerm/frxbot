name := "mtspredbot"
scalaVersion := "2.12.4"
version := "1.0"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scala-lang" % "scala-library" % "2.12.4",
  "com.typesafe" % "config" % "1.3.4",
  "com.bot4s" %% "telegram-core" % "4.2.0-RC1",
  "com.bot4s" %% "telegram-akka" % "4.2.0-RC1",
  "com.datastax.oss" % "java-driver-core" % "4.0.1",
  "com.github.jnr" % "jnr-ffi" % "2.1.10",
  "com.github.oshi" % "oshi-core" % "3.13.3"
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case "plugin.properties" => MergeStrategy.last
  case "log4j.properties" => MergeStrategy.last
  case "logback.xml" => MergeStrategy.last
  case "resources/logback.xml" => MergeStrategy.last
  case "resources/application.conf" => MergeStrategy.last
  case "resources/reference.conf" => MergeStrategy.last
  case "application.conf" => MergeStrategy.last
  case PathList("application.conf") => MergeStrategy.concat
  case PathList("reference.conf") => MergeStrategy.concat
  case "resources/control.conf" => MergeStrategy.discard
  case "control.conf" => MergeStrategy.discard
  case "resources/YOURPUBLIC.pem" => MergeStrategy.discard
  case "YOURPUBLIC.pem" => MergeStrategy.discard
  case x => MergeStrategy.first
}

assemblyJarName in assembly :="mtspredbot.jar"
mainClass in (Compile, packageBin) := Some("mtspredbot.Main")
mainClass in (Compile, run) := Some("mtspredbot.Main")
