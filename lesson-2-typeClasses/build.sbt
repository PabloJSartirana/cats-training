name         := "cats-training"
version      := "0.1.0"
scalaVersion := "3.3.6"

libraryDependencies ++= Seq(
  "org.typelevel"     %% "cats-core" % "2.13.0",
  "org.playframework" %% "play-json" % "3.0.4"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)

