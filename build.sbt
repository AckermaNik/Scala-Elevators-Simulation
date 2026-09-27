ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.16"

run / fork := false

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.20"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.11"
)

lazy val root = (project in file("."))
  .settings(
    name := "Elevator_Simulation"
  )
