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
import play.api.Logging
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.JsNull
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.ExternalWireMockSupport
import uk.gov.hmrc.mongo.cache.CacheIdType.SessionCacheId.NoSessionException
import uk.gov.hmrc.pegaproofofconceptfrontend.models.{AssignmentId, CaseId, SessionData, SessionId, StartCaseResponse}
import uk.gov.hmrc.pegaproofofconceptfrontend.repository.{CaseIdJourneyRepo, PegaSessionRepo}
import uk.gov.hmrc.pegaproofofconceptfrontend.testsupport.FakeApplicationProvider

import scala.concurrent.{ExecutionContext, Future}

class InputControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with ExternalWireMockSupport with FakeApplicationProvider
  with Logging {

  private val fakeAuthConnector = new AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
      Future.successful(JsNull.as[A](retrieval.reads))
    }
  }

  override val overrideModules: Seq[GuiceableModule] = Seq(bind[AuthConnector].toInstance(fakeAuthConnector))

  private val controller = app.injector.instanceOf[InputController]

  private val pegaSessionRepo = app.injector.instanceOf[PegaSessionRepo]

  private val caseIdJourneyRepo = app.injector.instanceOf[CaseIdJourneyRepo]

  "getStringInput" should {
    "return an error is no session id can be found" in {
      val error = intercept[Exception](await(controller.getStringInput()(FakeRequest())))
      error shouldBe a[NoSessionException.type]
    }

    "return 200" in {
      val result = controller.getStringInput()(FakeRequest().withSession("sessionId" -> "blah"))
      val doc: Document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      doc.select(".govuk-heading-xl").text() shouldBe "Send a string to PEGA"
    }
  }

  def createFormFilledFakeRequest(stringValue: String): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(Helpers.POST, routes.InputController.submitStringInput.path())
    .withFormUrlEncodedBody("string" -> stringValue).withSession("sessionId" -> "anything")

  "submitStringInput" should {
    "return 200" in {
      stubFor(
        post(urlPathEqualTo("/pega-proof-of-concept-proxy/start-case"))
          .willReturn(aResponse().withStatus(201).withBody(
            """
              |{
              |  "data": {
              |    "caseInfo": {
              |      "assignments": [
              |        {
              |          "ID": "ASSIGN-WORKBASKET HMRC-DEBT-WORK A-40026!STARTAFFORDABILITYASSESSMENT_FLOW"
              |        }
              |      ]
              |    }
              |  },
              |  "ID": "HMRC-DEBT-WORK A-40026"
              |}
              |""".stripMargin
          ))
      )

      val request = createFormFilledFakeRequest("nonEmptyString")
      val result = controller.submitStringInput()(request)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe
        Some("/pega-proof-of-concept/pega?caseId=HMRC-DEBT-WORK+A-40026&assignmentId=ASSIGN-WORKBASKET+HMRC-DEBT-WORK+A-40026%21STARTAFFORDABILITYASSESSMENT_FLOW")

      val expectedSessionData = SessionData(
        SessionId("anything"),
        "nonEmptyString",
        StartCaseResponse(
          StartCaseResponse.Data(
            StartCaseResponse.CaseInfo(
              List(StartCaseResponse.Assignment(AssignmentId("ASSIGN-WORKBASKET HMRC-DEBT-WORK A-40026!STARTAFFORDABILITYASSESSMENT_FLOW")))
            )
          ),
          CaseId("HMRC-DEBT-WORK A-40026")
        ),
        None
      )

      await(pegaSessionRepo.findSession(request)) shouldBe Some(expectedSessionData)
      await(caseIdJourneyRepo.findSession(CaseId("HMRC-DEBT-WORK A-40026"))) shouldBe Some(expectedSessionData)
    }

    "return a different status when returned with a different status from the controller" in {
      stubFor(
        post(urlPathEqualTo("/pega-proof-of-concept-proxy/start-case"))
          .willReturn(aResponse().withStatus(204))
      )

      val result = controller.submitStringInput()(createFormFilledFakeRequest("blah"))

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }
  }

}
