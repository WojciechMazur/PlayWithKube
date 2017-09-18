
name := "PlayWithKube"
 
version := "1.0" 
      
//lazy val `playwithkube` = (project in file(".")).enablePlugins(PlayScala)
lazy val root = (project in file(".")).enablePlugins(PlayScala,SbtWeb, SbtLess)


resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.2"


libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice )

libraryDependencies ++= Seq(
  "io.fabric8" % "kubernetes-client" % "2.6.3",
  "io.fabric8" % "kubernetes-model" % "1.1.4",
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"

)
unmanagedResourceDirectories in Test +=  baseDirectory ( _ /"target/web/public/test" ).value


