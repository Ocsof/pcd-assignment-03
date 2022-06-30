ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.2"

lazy val root = (project in file("."))
  .settings(
    name := "pcd-assignment-03"
  )

lazy val akkaVersion = "2.6.19"
lazy val akkaGroup = "com.typesafe.akka"
libraryDependencies ++= Seq(
  akkaGroup %% "akka-actor-typed" % akkaVersion,
  akkaGroup %% "akka-actor-testkit-typed" % akkaVersion % Test,
  akkaGroup %% "akka-cluster-typed" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
)
