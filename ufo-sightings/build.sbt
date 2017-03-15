val scalatestVersion = "3.0.1"

lazy val commonSettings = Seq(
  organization := "org.abhijitsarkar",
  version := "1.0-SNAPSHOT",
  scalaVersion := "2.11.8",
  scalacOptions := Seq(
    "-encoding",
    "UTF-8",
    "-target:jvm-1.8",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-unused-import",
    "-Ywarn-dead-code",
    "-Ywarn-infer-any",
    "-Ywarn-numeric-widen",
    "-Xfatal-warnings"
  ),
  libraryDependencies := Seq("org.scalatest" %% "scalatest" % scalatestVersion % Test),
  dependencyOverrides += "org.scalatest" %% "scalatest" % scalatestVersion
)

val sprayJsonVersion = "1.3.3"

lazy val commons = (project in file("commons"))
  .settings(commonSettings)
  .settings(libraryDependencies ++= Seq(
    "io.spray" %% "spray-json" % sprayJsonVersion
  ))

import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

val akkaVersion = "2.4.17"
val akkaStreamsKafkaVersion = "0.14"
val logbackVersion = "1.1.7"

lazy val akkaCommonDependencies = Seq(
  "com.typesafe.akka" %% "akka-stream-kafka" % akkaStreamsKafkaVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback" % "logback-core" % logbackVersion % Runtime,
  "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime
)

lazy val `akka-commons` = (project in file("akka-commons"))
  .settings(commonSettings)
  .settings(libraryDependencies ++= akkaCommonDependencies)

lazy val commonDockerSettings = Seq(
  dockerAlias := DockerAlias(dockerRepository.value, None, "ufo-sightings-" + name.value,
    Some((version in Docker).value)),
  assemblyMergeStrategy in assembly := {
    case x => {
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      val strategy = oldStrategy(x)
      if (strategy == MergeStrategy.deduplicate)
        MergeStrategy.first
      else strategy
    }
  },

  // Remove all jar mappings in universal and append the fat jar
  mappings in Universal := {
    val universalMappings = (mappings in Universal).value
    val fatJar = (assembly in Compile).value
    val filtered = universalMappings.filter {
      case (file, name) => !name.endsWith(".jar")
    }
    filtered :+ (fatJar -> ("lib/" + fatJar.getName))
  },

  dockerRepository := Some("asarkar")
  // packageName in Docker := "sightings-" + name.value,
  // Delete when fixed: https://github.com/sbt/sbt-native-packager/issues/947
  //  dockerAlias := DockerAlias(dockerRepository.value, None, "sightings-" + name.value, Some((version in Docker).value))
)

lazy val commonAkkaDockerSettings = commonDockerSettings ++ Seq(
  dockerCommands := Seq(
    Cmd("FROM", "openjdk:8u111-alpine"),
    Cmd("WORKDIR", "/"),
    Cmd("COPY", "opt/docker/lib/*.jar", "/app.jar"),
    Cmd("RUN", "sh", "-c", "'touch /app.jar'"),
    ExecCmd("ENTRYPOINT", "sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -jar /app.jar")
  )
)

val jsoupVersion = "1.10.2"
val akkaHttpVersion = "10.0.4"

lazy val `akka-producer` = (project in file("akka-producer"))
  .settings(commonSettings, commonAkkaDockerSettings)
  .settings(libraryDependencies ++= akkaCommonDependencies ++ Seq(
    "org.jsoup" % "jsoup" % jsoupVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test
  ))
  .dependsOn(commons, `akka-commons`)
  .enablePlugins(JavaAppPackaging)

lazy val `akka-consumer` = (project in file("akka-consumer"))
  .settings(commonSettings, commonAkkaDockerSettings)
  .settings(libraryDependencies ++= akkaCommonDependencies)
  .dependsOn(commons, `akka-commons`)
  .enablePlugins(JavaAppPackaging)

val configVersion = "1.3.1"
val sparkVersion = "2.1.0"

lazy val `spark-consumer` = (project in file("spark-consumer"))
  .settings(commonSettings, commonDockerSettings)
  .settings(libraryDependencies ++= Seq(
    "com.typesafe" % "config" % configVersion,
    "org.apache.spark" %% "spark-core" % sparkVersion % Provided,
    "org.apache.spark" %% "spark-streaming" % sparkVersion % Provided,
    "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkVersion
  ))
  .dependsOn(commons)
  .enablePlugins(JavaAppPackaging)
  .settings(Seq(
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false,
      includeDependency = true),

    dockerPackageMappings in Docker += (baseDirectory.value / "docker" / "spark-env.sh") -> "spark-env.sh",
    dockerPackageMappings in Docker += (baseDirectory.value / "docker" / "log4j.properties") -> "log4j.properties",

    // The default commands are shown by a) bin/activator shell b) show dockerCommands
    dockerCommands := Seq(
      Cmd("FROM", "asarkar/spark:2.1.0"),
      Cmd("WORKDIR", "/"),
      Cmd("COPY", "opt/docker/lib/*.jar", "/app.jar"),
      Cmd("COPY", "log4j.properties", "\"$SPARK_HOME\"/conf/"),
      Cmd("COPY", "spark-env.sh", "\"$SPARK_HOME\"/conf/"),
      Cmd("RUN", "chmod +x \"$SPARK_HOME/conf/spark-env.sh\""),
      ExecCmd("ENTRYPOINT", "/opt/spark/bin/spark-submit", "/app.jar")
    )
  ))

lazy val `ufo-sightings` = (project in file("."))
  .settings(commonSettings)
  .aggregate(`akka-producer`, `akka-consumer`, `spark-consumer`)

