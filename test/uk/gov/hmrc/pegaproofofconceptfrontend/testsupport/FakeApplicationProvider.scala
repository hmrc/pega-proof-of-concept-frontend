/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.pegaproofofconceptfrontend.testsupport

import org.scalatestplus.play.guice.GuiceFakeApplicationFactory
import play.api.Application
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import uk.gov.hmrc.http.test.ExternalWireMockSupport

trait FakeApplicationProvider { this: GuiceFakeApplicationFactory with ExternalWireMockSupport =>

  override def beforeEach(): Unit = {
    externalWireMockServer.resetAll()
  }

  val overrideModules: Seq[GuiceableModule] = Seq.empty

  val overrideConfig: Map[String, Any] = Map.empty

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(
        "metrics.jvm" -> false,
        "metrics.enabled" -> false,
        "microservice.services.pega-proof-of-concept-proxy.host" -> externalWireMockHost,
        "microservice.services.pega-proof-of-concept-proxy.port" -> externalWireMockPort
      )
      .configure(overrideConfig)
      .overrides(overrideModules: _*)
      .build()

}
