lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
  scalaVersion := "3.1.0" // latest: "3.1.1" (6 Mar 2022: missing go to definition with metals)
)

lazy val server = project
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= {
      val zHttpVersion = "2.0.0-RC4"
      val zioVersion = "2.0.0-RC2"

      Seq(
        "io.d11" %% "zhttp" % zHttpVersion,
        "dev.zio" %% "zio-test" % zioVersion % Test,
        "dev.zio" %% "zio-test-sbt" % zioVersion % Test
      )
    },
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )

lazy val loadtest = project
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= {
      val gatlingVersion = "3.7.6"

      Seq(
        "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion,
        "io.gatling" % "gatling-test-framework" % gatlingVersion
      )
    }
  )
  .enablePlugins(GatlingPlugin)

lazy val root =
  project
    .in(file("."))
    .settings(commonSettings)
    .dependsOn(server, loadtest)
    .aggregate(server, loadtest)
