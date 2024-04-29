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

import cats.syntax.eq._
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.pegaproofofconceptfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.pegaproofofconceptfrontend.connectors.PegaProxyConnector
import uk.gov.hmrc.pegaproofofconceptfrontend.controllers.actions.{AuthenticatedAction, AuthenticatedRequest}
import uk.gov.hmrc.pegaproofofconceptfrontend.models.StringForm.createStringInputForm
import uk.gov.hmrc.pegaproofofconceptfrontend.models._
import uk.gov.hmrc.pegaproofofconceptfrontend.repository.{CaseIdJourneyRepo, PegaSessionRepo}
import uk.gov.hmrc.pegaproofofconceptfrontend.repository.PegaSessionRepo.toSessionId
import uk.gov.hmrc.pegaproofofconceptfrontend.views.Views
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import java.net.URLEncoder
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InputController @Inject() (
    mcc:                MessagesControllerComponents,
    authenticateUser:   AuthenticatedAction,
    views:              Views,
    errorHandler:       ErrorHandler,
    pegaProxyConnector: PegaProxyConnector,
    sessionRepo:        PegaSessionRepo,
    caseIdJourneyRepo:  CaseIdJourneyRepo,
    appConfig:          AppConfig
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc) with Logging with I18nSupport {

  val getStringInput: Action[AnyContent] = Action.andThen[AuthenticatedRequest](authenticateUser) { implicit request =>
    Ok(views.stringInputPage(createStringInputForm()))
  }

  def urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

  val submitStringInput: Action[AnyContent] = Action.andThen[AuthenticatedRequest](authenticateUser).async { implicit request =>
    createStringInputForm().bindFromRequest().fold(
      formWithErrors =>
        Future.successful((BadRequest(views.stringInputPage(formWithErrors)))),
      (validFormData: StringInputForm) =>
        pegaProxyConnector.startCase().flatMap {
          case response if response.status === CREATED =>
            logger.info(s"[OPS-11581] SUBMITTED STRING: '${validFormData.string}' TO PEGA")
            val startCaseResponse = response.json.as[StartCaseResponse]

            updateMongo(validFormData, startCaseResponse).map {
              _ =>
                val assignmentId = startCaseResponse.data.caseInfo.assignments.headOption.map(_.ID).getOrElse(sys.error("Could not find assignment id"))
                val queryString: String = s"?caseId=${urlEncode(startCaseResponse.ID.value)}&assignmentId=${urlEncode(assignmentId.value)}"
                Redirect(appConfig.Urls.pegaRedirectUrl + queryString)
            }
          case response =>
            logger.warn(s"[OPS-11581] failure to connect to proxy response status: " + response.status.toString + " - response body: " + response.body)
            Future.successful(InternalServerError(errorHandler.internalServerErrorTemplate))
        }
    )
  }

  private def updateMongo(formData: StringInputForm, startCaseResponse: StartCaseResponse)(implicit request: Request[_]): Future[Unit] = {
    val sessionData = SessionData(toSessionId(request), formData.string, startCaseResponse, None)

    for {
      _ <- sessionRepo.upsert(sessionData)
      _ <- caseIdJourneyRepo.insertSession(startCaseResponse.ID, sessionData)
    } yield ()
  }

  val signOut: Action[AnyContent] = Action { _ =>
    Redirect(appConfig.Urls.signOutUrl).withNewSession
  }

}
