/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.pegaproofofconceptfrontend.controllers.actions

import com.google.inject.Inject
import play.api.Logging
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.pegaproofofconceptfrontend.config.AppConfig

import scala.concurrent.{ExecutionContext, Future}

final class AuthenticatedRequest[A](val request: MessagesRequest[A]) extends MessagesRequest[A](request, request.messagesApi)

class AuthenticatedAction @Inject() (
    af:        AuthorisedFunctions,
    cc:        MessagesControllerComponents,
    appConfig: AppConfig
)(
    implicit
    ec: ExecutionContext
) extends ActionRefiner[MessagesRequest, AuthenticatedRequest] with Logging {

  import uk.gov.hmrc.pegaproofofconceptfrontend.req.RequestSupport._

  override protected def refine[A](request: MessagesRequest[A]): Future[Either[Result, AuthenticatedRequest[A]]] = {
    implicit val r: Request[A] = request

    af.authorised().apply {
      Future.successful(
        Right(new AuthenticatedRequest[A](request))
      )
    }
      .recover {
        case _: NoActiveSession =>
          Left(Redirect(appConfig.Urls.loginUrl, Map("continue" -> Seq(appConfig.BaseUrl.pegaPocFrontend + request.uri))))
      }
  }

  override protected def executionContext: ExecutionContext = cc.executionContext

}

