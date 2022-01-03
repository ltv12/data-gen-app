name := "final-project"

version := "0.1"

scalaVersion := "2.13.7"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-Ymacro-annotations"
)

val catsVersion = "2.6.1"
val catsTaglessVersion = "0.11"
val catsEffectVersion = "2.5.1"
val scalaTestVersion = "3.2.7.0"
val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-effect" % catsEffectVersion,
  "org.scalatest" %% "scalatest" % "3.2.10" % Test,
  "io.circe" %% "circe-yaml" % "0.14.1",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion
)

//addCompilerPlugin("org.typelevel" %% "kind-proje
// tor" % "0.11.1" cross CrossVersion.full)

run / fork := true
run / connectInput := true
run / outputStrategy := Some(StdoutOutput)
