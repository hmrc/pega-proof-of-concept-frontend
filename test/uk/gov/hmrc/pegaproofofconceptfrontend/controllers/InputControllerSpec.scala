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

package uk.gov.hmrc.pegaproofofconceptfrontend.controllers

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathEqualTo}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.JsNull
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.ExternalWireMockSupport
import uk.gov.hmrc.pegaproofofconceptfrontend.testsupport.FakeApplicationProvider
import uk.gov.hmrc.pegaproofofconceptfrontend.utils.Generators
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.mvc.AnyContentAsFormUrlEncoded

import scala.concurrent.{ExecutionContext, Future}

class InputControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with ExternalWireMockSupport with FakeApplicationProvider
  with Generators with ScalaCheckDrivenPropertyChecks {

  private val fakeAuthConnector = new AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
      Future.successful(JsNull.as[A](retrieval.reads))
    }
  }

  override val overrideModules: Seq[GuiceableModule] = Seq(bind[AuthConnector].toInstance(fakeAuthConnector))

  private val controller = app.injector.instanceOf[InputController]

  private val fakeRequest = FakeRequest()

  "getStringInput" should {
    "return 200" in {
      val result = controller.getStringInput()(fakeRequest)
      val doc: Document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      doc.select(".govuk-heading-xl").text() shouldBe "Send a string to PEGA"
    }
  }

  def createFormFilledFakeRequest(stringValue: String): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(Helpers.POST, routes.InputController.submitStringInput.path())
    .withFormUrlEncodedBody("string" -> stringValue)

  "submitStringInput" should {
    "return 200" in {
      forAll(nonEmptyStringGen) {nonEmptyString =>
        stubFor(
          post(urlPathEqualTo("/pega-proof-of-concept-proxy/submit-payload"))
            .willReturn(aResponse().withStatus(200))
        )

        val result = controller.submitStringInput()(createFormFilledFakeRequest(nonEmptyString))
        status(result) shouldBe Status.OK

      }
    }
    "return a different status when returned with a different status from the controller" in {
      forAll(nonEmptyStringGen) { nonEmptyString =>
        stubFor(
          post(urlPathEqualTo("/pega-proof-of-concept-proxy/submit-payload"))
            .willReturn(aResponse().withStatus(204))
        )

        externalWireMockServer.getStubMappings.forEach(println(_))

        val result = controller.submitStringInput()(createFormFilledFakeRequest(nonEmptyString))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some("/pega-proof-of-concept/problem-with-the-service")
      }
    }
  }

}
