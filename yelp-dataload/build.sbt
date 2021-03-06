name := "yelp-dataload"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

enablePlugins(JavaAppPackaging, DockerPlugin)

scalacOptions := Seq(
  "-feature", "-unchecked", "-deprecation", "-encoding", "utf8"
)
val sparkVersion = "2.1.0"
val scalatestVersion = "3.0.1"
val jacksonVersion = "2.8.7"
val configVersion = "1.3.1"

libraryDependencies ++= Seq(
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.couchbase.client" %% "spark-connector" % sparkVersion,
  "com.typesafe" % "config" % configVersion,
  "org.apache.spark" %% "spark-core" % sparkVersion % Provided,
  "org.apache.spark" %% "spark-streaming" % sparkVersion % Provided,
  "org.apache.spark" %% "spark-sql" % sparkVersion % Provided,
  "org.scalatest" %% "scalatest" % scalatestVersion % Test
)

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false, includeDependency = true)

assemblyMergeStrategy in assembly := {
  case x => {
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    val strategy = oldStrategy(x)
    if (strategy == MergeStrategy.deduplicate)
      MergeStrategy.first
    else strategy
  }
}

// Remove all jar mappings in universal and append the fat jar
mappings in Universal := {
  val universalMappings = (mappings in Universal).value
  val fatJar = (assembly in Compile).value
  val filtered = universalMappings.filter {
    case (file, name) => !name.endsWith(".jar")
  }
  filtered :+ (fatJar -> ("lib/" + fatJar.getName))
}

dockerRepository := Some("asarkar")
dockerPackageMappings in Docker += (baseDirectory.value / "docker" / "spark-env.sh") -> "spark-env.sh"
dockerPackageMappings in Docker += (baseDirectory.value / "docker" / "log4j.properties") -> "log4j.properties"

// The default commands are shown by a) bin/activator shell b) show dockerCommands
import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}
dockerCommands := Seq(
  Cmd("FROM", "asarkar/spark:2.1.0"),
  Cmd("WORKDIR", "/"),
  Cmd("COPY", "opt/docker/lib/*.jar", "/app.jar"),
  Cmd("COPY", "log4j.properties", "\"$SPARK_HOME\"/conf/"),
  Cmd("COPY", "spark-env.sh", "\"$SPARK_HOME\"/conf/"),
  Cmd("RUN", "chmod +x \"$SPARK_HOME/conf/spark-env.sh\""),
  ExecCmd("ENTRYPOINT", "/opt/spark/bin/spark-submit", "/app.jar")
)
