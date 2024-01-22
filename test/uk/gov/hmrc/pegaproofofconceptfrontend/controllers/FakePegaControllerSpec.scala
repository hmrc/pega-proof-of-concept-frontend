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
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsNull
import play.api.test.Helpers._
import play.api.test.{FakeRequest}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class FakePegaControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {
  private val fakeAuthConnector = new AuthConnector {
    override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {
      Future.successful(JsNull.as[A](retrieval.reads))
    }
  }

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(bind[AuthConnector].toInstance(fakeAuthConnector))
      .configure(
        "metrics.jvm" -> false,
        "metrics.enabled" -> false
      )
      .build()

  private val controller = app.injector.instanceOf[FakePegaController]

  private val fakeRequest = FakeRequest()

  "fakePegaPage" should {
    "return 200" in {
      val result = controller.fakePegaPage()(fakeRequest)
      val doc: Document = Jsoup.parse(contentAsString(result))

      status(result) shouldBe Status.OK
      contentType(result) shouldBe Some("text/html")
      doc.select(".govuk-heading-xl").text() shouldBe "Fake Pega Page"
    }
  }

}
