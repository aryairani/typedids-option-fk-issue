import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "typedids-option-fk-issue"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    "com.typesafe.slick" %% "slick-extensions" % "1.0.0",
    "com.typesafe.play" %% "play-slick" % "0.3.3"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
