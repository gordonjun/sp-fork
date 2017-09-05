enablePlugins(ScalaJSPlugin)

name := "spgui"

version := "2.1"

scalaOrganization := "org.scala-lang"

scalaVersion := "2.12.3"

//javaOptions += "-Xmx2048m"

scalacOptions  := Seq(
  "-encoding", "utf8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-language:implicitConversions",
  "-language:postfixOps"
)

val scalaJSReactVersion = "1.1.0"
val scalaCssVersion = "0.5.3"
val diodeVersion = "1.1.2"
val scalajsGoogleChartsVersion = "0.4.1"

resolvers += sbt.Resolver.url("aleastchs Bintray Releases", url("https://dl.bintray.com/aleastchs/aleastChs-releases"))(Resolver.ivyStylePatterns)

libraryDependencies ++= Seq(
  "com.github.japgolly.scalajs-react" %%% "core" % scalaJSReactVersion,
  "com.github.japgolly.scalajs-react" %%% "extra" % scalaJSReactVersion,
  "com.github.japgolly.scalacss" %%% "core" % scalaCssVersion,
  "com.github.japgolly.scalacss" %%% "ext-react" % scalaCssVersion,
  "io.suzaku" %%% "diode" % diodeVersion,
  "io.suzaku" %%% "diode-react" % diodeVersion,
  "com.lihaoyi" %%% "scalarx" % "0.3.2",
  "org.singlespaced" %%% "scalajs-d3" % "0.3.4",
  "org.scalatest" %%% "scalatest" % "3.0.1" % "test",
  "org.scala-lang.modules" %%% "scala-parser-combinators" % "1.0.5",
  "com.typesafe.play" %%% "play-json" % "2.6.0",
  "org.julienrf" %%% "play-json-derived-codecs" % "4.0.0",
  "io.github.cquiroz" %%% "scala-java-time" % "2.0.0-M12",
  "org.aleastChs" % "scalajs-google-charts_2.12" % scalajsGoogleChartsVersion
)

libraryDependencies += "org.scala-js" %%% "scalajs-java-time" % "0.2.2"


//scalajs-bundler
enablePlugins(ScalaJSBundlerPlugin)

webpackConfigFile in fastOptJS := Some(baseDirectory.value / "npmdependencies/webpack.config.js")

npmDependencies in Compile += "bootstrap" -> "3.3.7"
//  dependencies": {
//  "bootstrap": "~3.3.7",
//  "chart.js": "~2.5.0",
//  "paths-js": "~0.4.5",
//  "font-awesome": "~4.7.0",
//  "jquery": "~3.1.1",
//  "jsoneditor": "~5.6.0",
//  "react": "~15.3.2",
//  "react-dom": "~15.3.2",
//  "react-grid-layout": "~0.13.9"
//  },
//  "devDependencies": {
//  "css-loader": "~0.26.1",
//  "file-loader": "~0.9.0",
//  "json-loader": "~0.5.4",
//  "style-loader": "~0.13.1",
//  "url-loader": "~0.5.7",
//  "webpack": "~1.14.0",
//  "webpack-uglify-js-plugin": "~1.1.9"

/* This is how to include js files. Put it in src/main/resources.
jsDependencies ++= Seq(
  ProvidedJS / "SomeJSFile.js"
)
*/
