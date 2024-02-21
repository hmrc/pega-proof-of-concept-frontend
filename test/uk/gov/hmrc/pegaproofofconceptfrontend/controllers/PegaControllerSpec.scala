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
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.JsNull
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.ExternalWireMockSupport
import uk.gov.hmrc.pegaproofofconceptfrontend.models.{SessionData, SessionId, StartJourneyResponseModel}
import uk.gov.hmrc.pegaproofofconceptfrontend.repository.PegaSessionRepo
import uk.gov.hmrc.pegaproofofconceptfrontend.testsupport.FakeApplicationProvider
import uk.gov.hmrc.pegaproofofconceptfrontend.utils.Generators

import scala.concurrent.{ExecutionContext, Future}

class PegaControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with ExternalWireMockSupport with FakeApplicationProvider
  with Generators with ScalaCheckDrivenPropertyChecks {

  private val fakeAuthConnector = new AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
      Future.successful(JsNull.as[A](retrieval.reads))
    }
  }

  override val overrideModules: Seq[GuiceableModule] = Seq(bind[AuthConnector].toInstance(fakeAuthConnector))

  private val controller = app.injector.instanceOf[PegaController]

  private val fakeRequest = FakeRequest().withSession("sessionId" -> "anything")

  private val pegaSessionRepo = app.injector.instanceOf[PegaSessionRepo]

  "pegaPage" should {
    "open the fake pega page" in {
      val upsertResult = pegaSessionRepo.upsert(SessionData(
        SessionId("anything"),
        "nonEmptyString",
        StartJourneyResponseModel(
          "HMRC-DEBT-WORK A-13002",
          "ASSIGN-WORKLIST HMRC-DEBT-WORK A-13002!STARTAFFORDABILITYASSESSMENT_FLOW",
          "Perform",
          "Pega-API-CaseManagement-Case"
        )
      ))
      await(upsertResult) shouldBe (())
      val result = controller.pegaPage(fakeRequest)

      status(result) shouldBe Status.OK

      val doc: Document = Jsoup.parse(contentAsString(result))

      doc.select("form").attr("action") shouldBe "/pega-proof-of-concept/callback/HMRC-DEBT-WORK%20A-13002"
    }
  }
}
