


lazy val akkaHttpVersion = "10.1.7"
lazy val akkaVersion    = "2.5.21"
lazy val log4jApiScalaVersion = "11.0"
lazy val log4jVersion = "2.11.0"
lazy val scalaLoggingVersion = "3.9.2"
lazy val circeVersion = "0.11.1"
lazy val pureconfigVersion = "0.10.2"
lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.github.zavalit",
      scalaVersion    := "2.12.8",
      name            := "journeyplanner"
    )),
    name := "journeyplanner",
    libraryDependencies ++= Seq(
      "com.typesafe.akka"       %% "akka-http"             % akkaHttpVersion,
      "com.typesafe.akka"       %% "akka-stream-typed"     % akkaVersion,
      "com.github.pureconfig"   %% "pureconfig"            % pureconfigVersion,
      "io.circe"                %% "circe-core"            % circeVersion,
      "io.circe"                %% "circe-jawn"            % circeVersion,
      "io.circe"                %% "circe-generic"         % circeVersion,
      "ch.qos.logback"          % "logback-classic"        % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
      "com.typesafe.akka"       %% "akka-http-testkit"     % akkaHttpVersion % Test,
      "com.typesafe.akka"       %% "akka-testkit"          % akkaVersion     % Test,
      "com.typesafe.akka"       %% "akka-stream-testkit"   % akkaVersion     % Test,
      "com.typesafe.akka"       %% "akka-actor-testkit-typed"    % akkaVersion     % Test,
      "org.scalatest"           %% "scalatest"             % "3.0.5"         % Test
    )
  )


fork := true