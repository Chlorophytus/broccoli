ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "0.2.0"
ThisBuild / organization := "com.github.chlorophytus"

val chiselVersion = "3.6.0"

lazy val root = (project in file("."))
  .settings(
    name := "broccoli",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % chiselVersion,
      "edu.berkeley.cs" %% "chiseltest" % "0.6.0" % "test",
      "org.slf4j" % "slf4j-simple" % "2.0.7",
      "org.slf4j" % "slf4j-api" % "2.0.7",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit"
    ),
    addCompilerPlugin(
      "edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full
    )
  )
