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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlPathEqualTo}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsNull, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.ExternalWireMockSupport
import uk.gov.hmrc.mongo.cache.CacheIdType.SessionCacheId.NoSessionException
import uk.gov.hmrc.pegaproofofconceptfrontend.models.{CaseId, SessionData, SessionId, StartCaseResponse}
import uk.gov.hmrc.pegaproofofconceptfrontend.repository.{CaseIdJourneyRepo, PegaSessionRepo}
import uk.gov.hmrc.pegaproofofconceptfrontend.testsupport.FakeApplicationProvider

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class CallbackControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with ExternalWireMockSupport with FakeApplicationProvider with BeforeAndAfterEach {

  private val fakeAuthConnector = new AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
      Future.successful(JsNull.as[A](retrieval.reads))
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(pegaSessionRepo.collection.drop().toFuture().map(_ => ())) shouldBe (())
    await(caseIdJourneyRepo.collection.drop().toFuture().map(_ => ())) shouldBe (())
    ()
  }

  override val overrideModules: Seq[GuiceableModule] = Seq(bind[AuthConnector].toInstance(fakeAuthConnector))

  private val controller = app.injector.instanceOf[CallbackController]

  private val pegaSessionRepo = app.injector.instanceOf[PegaSessionRepo]

  private val caseIdJourneyRepo = app.injector.instanceOf[CaseIdJourneyRepo]

  private val fakeRequest = FakeRequest().withSession("sessionId" -> "blah")

  "callback" should {

    val responseJson = Json.parse(
      """
        |{
        |  "a":"b"
        |}
        |""".stripMargin
    )

    val caseId = CaseId("id")

    val initialSessionData = SessionData(
      SessionId("not-the-same-session-id-in-request"),
      "beans",
      StartCaseResponse(caseId, "assignmentId", "pageId", "objclass"),
      None
    )

    "redirect to returns when given a caseId that matches mongo and the case is retrieved successfully" in {
      val mongoUpsertResult = caseIdJourneyRepo.insertSession(caseId, initialSessionData)
      await(mongoUpsertResult) shouldBe (())

      stubFor(
        get(urlPathEqualTo(s"/pega-proof-of-concept-proxy/case/${caseId.value}"))
          .willReturn(aResponse().withStatus(200).withBody(responseJson.toString()))
      )

      val result = controller.callback(caseId)(fakeRequest)

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some("/pega-proof-of-concept/return")

      val mongoFindResult = pegaSessionRepo.findSession(fakeRequest)
      await(mongoFindResult) shouldBe Some(SessionData(
        SessionId("blah"),
        "beans",
        StartCaseResponse(CaseId("id"), "assignmentId", "pageId", "objclass"),
        Some(responseJson)
      ))
    }

    "return an error when given a caseId that matches mongo but the case is not retrieved successfully" in {

      val mongoUpsertResult = caseIdJourneyRepo.insertSession(caseId, initialSessionData)
      await(mongoUpsertResult) shouldBe (())

      stubFor(
        get(urlPathEqualTo("/pega-proof-of-concept-proxy/case/id"))
          .willReturn(aResponse().withStatus(204).withBody(responseJson.toString()))
      )

      val result = controller.callback(caseId)(fakeRequest)

      val exception = intercept[Exception](await(result))
      exception.getMessage shouldBe "call to get case came back with 204"
    }

    "return an error when given a caseId that does not match with mongo" in {
      val otherCaseId = CaseId("beans")
      val mongoUpsertResult = caseIdJourneyRepo.insertSession(otherCaseId, initialSessionData)
      await(mongoUpsertResult) shouldBe (())

      val result = controller.callback(otherCaseId)(fakeRequest)

      val exception = intercept[Exception](await(result))
      exception.getMessage shouldBe "query parameter beans did not match caseId in mongo, id"
    }

    "return an error when no sessionId is found" in {
      val mongoUpsertResult = caseIdJourneyRepo.insertSession(caseId, initialSessionData)
      await(mongoUpsertResult) shouldBe (())

      val result = controller.callback(caseId)(FakeRequest())

      val exception = intercept[Exception](await(result))
      exception shouldBe NoSessionException
    }

    "return an error when no data is found in the caseIdJourney repo" in {
      val result = controller.callback(caseId)(fakeRequest)

      val exception = intercept[Exception](await(result))
      exception.getMessage shouldBe "Could not find session data for case ID 'id'"
    }

  }

  "returns" should {
    "open to the returns page" in {

      val sessionJson: JsValue = Json.parse(
        """
          |{
          |"name":"John Smith"
          |}
          |""".stripMargin
      )

      val initialSessionData = SessionData(
        SessionId("blah"),
        "beans",
        StartCaseResponse(CaseId("id"), "assignmentId", "pageId", "objclass"),
        Some(sessionJson)
      )

      val mongoUpsertResult = pegaSessionRepo.upsert(initialSessionData)
      await(mongoUpsertResult) shouldBe (())

      val result = controller.returns()(fakeRequest)
      val doc: Document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe Status.OK
      doc.select(".govuk-heading-xl").text() shouldBe "MDTP return page"
      doc.select(".govuk-body").text() shouldBe "Session data is:"
      Json.parse(doc.select(".jsonBody").text()) shouldBe Json.toJson(initialSessionData)
    }
  }
}
