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

package uk.gov.hmrc.pegaproofofconceptfrontend.controllers

import play.api.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Request, Result}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler

import javax.inject.{Inject, Singleton}

@Singleton
class ErrorHandler @Inject() (

    errorTemplate:            uk.gov.hmrc.pegaproofofconceptfrontend.views.html.ErrorTemplate,
    override val messagesApi: MessagesApi
) extends FrontendErrorHandler {

  override def standardErrorTemplate(
      pageTitle: String,
      heading:   String,
      message:   String
  )(
      implicit
      request: Request[_]
  ): Html =
    errorTemplate(
      pageTitle,
      heading, message
    )
}

object ErrorHandler {

  private val logger = Logger(getClass)

  def technicalDifficulties()(implicit request: Request[_]): Result = {
    logger.info("Redirecting to 'Technical difficulties' on error")
    throw new RuntimeException("Something went wrong. Inspect stack trace and fix bad code")
  }

}
