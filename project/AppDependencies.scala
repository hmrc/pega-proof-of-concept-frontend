import sbt.*

object AppDependencies {

  private val bootstrapVersion = "8.4.0"
  private val hmrcMongoVersion = "1.7.0"

  val compile = Seq(
    // format: OFF
    "uk.gov.hmrc"             %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"             %% "play-frontend-hmrc-play-30" % "8.3.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-30"         % hmrcMongoVersion
  // format: ON
  )

  val test = Seq(
    // format: OFF
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"     % bootstrapVersion    % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30"    % hmrcMongoVersion    % Test,
    "org.jsoup"               %  "jsoup"                      % "1.17.2"            % Test,
  // format: ON
  )

  // only add additional dependencies here - it test inherit test dependencies above already
  val it: Seq[ModuleID] = Seq.empty
}
