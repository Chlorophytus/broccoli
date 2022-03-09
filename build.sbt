// See README.md for license details.

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "0.2.0"
ThisBuild / organization := "us.chlorophyt"

lazy val root = (project in file("."))
  .settings(
    name := "broccoli",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % "3.5.1",
      "edu.berkeley.cs" %% "chiseltest" % "0.5.0" % "test"
    ),
    scalacOptions ++= Seq(
      "-Xsource:2.13",
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit"
    ),
    addCompilerPlugin(
      "edu.berkeley.cs" % "chisel3-plugin" % "3.5.1" cross CrossVersion.full
    )
  )
