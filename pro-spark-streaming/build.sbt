name := "pro-spark-streaming"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

// All Spark Packages need a license
//licenses := Seq("Apache-2.0" -> url("http://opensource.org/licenses/Apache-2.0"))

// Needed as SBT's classloader doesn't work well with Spark
//fork := true

// BUG: unfortunately, it's not supported right now
//fork in console := true

scalacOptions := Seq(
  "-feature", "-unchecked", "-deprecation", "-encoding", "utf8"
)
val sparkVersion = "2.1.0"
val scalatestVersion = "3.0.1"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % Provided,
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
