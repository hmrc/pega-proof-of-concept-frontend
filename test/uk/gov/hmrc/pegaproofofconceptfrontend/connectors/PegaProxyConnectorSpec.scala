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

package uk.gov.hmrc.pegaproofofconceptfrontend.connectors

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.pegaproofofconceptfrontend.config.AppConfig
import org.mockito.scalatest.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.pegaproofofconceptfrontend.utils.{ConnectorSpec, Generators, WireMockHelper}
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext

class PegaProxyConnectorSpec extends AnyWordSpec with GuiceOneAppPerSuite with MockitoSugar with Generators with ScalaCheckDrivenPropertyChecks with Matchers with ConnectorSpec
  with WireMockHelper with ScalaFutures {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "metrics.jvm" -> false,
        "metrics.enabled" -> false
      )
      .build()

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    val mockConfig: AppConfig = mock[AppConfig]
    //    when(mockConfig.voiceBiometricsProxy).thenReturn(VoiceBiometricsProxyConfig("http://some-url", true))
    val mockHttp: HttpClientV2 = mock[HttpClientV2]
    val connector = new PegaProxyConnector(mockHttp, mockConfig)
  }

  "PegaProxyConnector" should {
    "submitPayloadToProxy returns 200 return the same http response" in new Setup {
      when(mockConfig.Urls).thenReturn(AppConfig.Urls(""))
      when(mockConfig.BaseUrl.pegaProxy).thenReturn("someBaseUrl")
      stubGet("someBaseUrl/somePegaUrl", 200, None)

      forAll(nonEmptyPayload) { payload =>
        val result = connector.submitPayloadToProxy(payload)
        result.futureValue === HttpResponse
      }

    }
    "submitPayloadToProxy returns 200 any other HttpResponse return the same http response" in {

    }
  }

}
