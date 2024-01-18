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
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class InputControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

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

  private val formFilledFakeRequest = FakeRequest(Helpers.POST, routes.InputController.submitStringInput.path())
    .withFormUrlEncodedBody("string" -> "someString")

  "submitStringInput" should {
    "return 303" in {
      val result = controller.submitStringInput()(formFilledFakeRequest)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

}
