// See README.md for license details.

ThisBuild / scalaVersion := "2.12.12"
ThisBuild / version := "0.2.0"
ThisBuild / organization := "com.github.chlorophytus"

lazy val root = (project in file("."))
  .settings(
    name := "broccoli",
    libraryDependencies ++= Seq(
      "edu.berkeley.cs" %% "chisel3" % "3.4.0",
      "edu.berkeley.cs" %% "chiseltest" % "0.3.0" % "test"
    ),
    scalacOptions ++= Seq(
      "-Xsource:2.11",
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit"
    ),
    addCompilerPlugin(
      "edu.berkeley.cs" % "chisel3-plugin" % "3.4.0" cross CrossVersion.full
    ),
    addCompilerPlugin(
      "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
    )
  )
