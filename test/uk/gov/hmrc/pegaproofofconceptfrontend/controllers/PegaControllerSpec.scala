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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.JsNull
import play.api.mvc.Session
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.ExternalWireMockSupport
import uk.gov.hmrc.mongo.cache.CacheIdType.SessionCacheId.NoSessionException
import uk.gov.hmrc.pegaproofofconceptfrontend.models.{CaseId, SessionData, SessionId, StartCaseResponse}
import uk.gov.hmrc.pegaproofofconceptfrontend.repository.PegaSessionRepo
import uk.gov.hmrc.pegaproofofconceptfrontend.testsupport.FakeApplicationProvider

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class PegaControllerSpec extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with ExternalWireMockSupport
  with FakeApplicationProvider
  with BeforeAndAfterEach {

  private val fakeAuthConnector = new AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
      Future.successful(JsNull.as[A](retrieval.reads))
    }
  }

  override val overrideModules: Seq[GuiceableModule] = Seq(bind[AuthConnector].toInstance(fakeAuthConnector))

  private val controller = app.injector.instanceOf[PegaController]

  private val fakeRequest = FakeRequest().withSession("sessionId" -> "anything")

  private val pegaSessionRepo = app.injector.instanceOf[PegaSessionRepo]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(pegaSessionRepo.collection.drop().toFuture().map(_ => ())) shouldBe (())
    ()
  }

  "pegaPage" should {
    "open the fake pega page" in {
      val result = controller.pegaPage(fakeRequest)

      status(result) shouldBe Status.OK

      val doc: Document = Jsoup.parse(contentAsString(result))

      doc.select("form").attr("action") shouldBe "/pega-proof-of-concept/pega"
    }
  }

  "pegaPageContinue" should {
    "return an error if no session data is found" in {
      val error = intercept[Exception](await(controller.pegaPageContinue(fakeRequest)))
      error shouldBe a[NoSessionException.type]
    }

    "redirect to the callback endpoint whilst clearing the cookie session" in {
      val upsertResult = pegaSessionRepo.upsert(SessionData(
        SessionId("anything"),
        "nonEmptyString",
        StartCaseResponse(
          CaseId("HMRC-DEBT-WORK A-13002"),
          "ASSIGN-WORKLIST HMRC-DEBT-WORK A-13002!STARTAFFORDABILITYASSESSMENT_FLOW",
          "Perform",
          "Pega-API-CaseManagement-Case"
        ),
        None
      ))
      await(upsertResult) shouldBe (())

      val result = controller.pegaPageContinue(fakeRequest)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/pega-proof-of-concept/callback?p=HMRC-DEBT-WORK+A-13002")
      session(result) shouldBe Session()
    }

  }
}
