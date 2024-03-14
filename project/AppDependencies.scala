import sbt.*

object AppDependencies {

  private val bootstrapVersion = "8.5.0"
  private val hmrcMongoVersion = "1.7.0"
  private val catsVersion = "2.10.0"

  val compile = Seq(
    // format: OFF
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc-play-30" % "9.0.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "org.typelevel"           %% "cats-core"                  % catsVersion
  // format: ON
  )

  val test = Seq(
    // format: OFF
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion    % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % hmrcMongoVersion    % Test,
    "org.jsoup"               %  "jsoup"                      % "1.17.2"            % Test,
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "5.1.0"             % Test,
    "org.scalatestplus"       %% "scalacheck-1-17"            % "3.2.18.0"          % Test,
    "org.scalacheck"          %% "scalacheck"                 % "1.17.0"            % Test,
    "org.mockito"             %% "mockito-scala-scalatest"    % "1.17.30"           % Test
  // format: ON
  )

  // only add additional dependencies here - it test inherit test dependencies above already
  val it: Seq[ModuleID] = Seq.empty
}
