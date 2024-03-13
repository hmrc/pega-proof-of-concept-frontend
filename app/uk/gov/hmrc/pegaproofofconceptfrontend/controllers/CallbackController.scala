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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.mongo.cache.CacheIdType.SessionCacheId.NoSessionException
import uk.gov.hmrc.pegaproofofconceptfrontend.connectors.PegaProxyConnector
import uk.gov.hmrc.pegaproofofconceptfrontend.controllers.actions.{AuthenticatedAction, AuthenticatedRequest}
import uk.gov.hmrc.pegaproofofconceptfrontend.models.{CaseId, SessionData}
import uk.gov.hmrc.pegaproofofconceptfrontend.repository.{CaseIdJourneyRepo, PegaSessionRepo}
import uk.gov.hmrc.pegaproofofconceptfrontend.views.Views
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CallbackController @Inject() (
    mcc:               MessagesControllerComponents,
    authenticateUser:  AuthenticatedAction,
    sessionRepo:       PegaSessionRepo,
    caseIdJourneyRepo: CaseIdJourneyRepo,
    connector:         PegaProxyConnector,
    views:             Views
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc) with Logging with I18nSupport {

  def callback(caseId: CaseId): Action[AnyContent] = Action.andThen[AuthenticatedRequest](authenticateUser).async { implicit request =>
    logger.info(s"query parameter was ${caseId.value}")

    reconstructSessionData(caseId).flatMap(sessionData =>
      if (caseId === sessionData.pegaJourneyResponse.ID)
        connector.getCase(caseId).flatMap {
        case response if response.status === OK =>
          sessionRepo.upsert(sessionData.copy(getCaseResponse = Some(response.json))).map(_ =>
            Redirect(routes.CallbackController.returns))
        case other =>
          sys.error(s"call to get case came back with ${other.status.toString}")
      }
      else
        sys.error(s"query parameter ${caseId.value} did not match caseId in mongo, ${sessionData.pegaJourneyResponse.ID.value}"))
  }

  // session ID changes on callback from PEGA so need to reconstruct session data
  private def reconstructSessionData(caseId: CaseId)(implicit request: Request[_]): Future[SessionData] = {
    caseIdJourneyRepo.findSession(caseId).map{
      case Some(sessionData) =>
        sessionData.copy(sessionId = PegaSessionRepo.toSessionId(request))

      case None =>
        sys.error(s"Could not find session data for case ID '${caseId.value}'")
    }
  }

  val returns: Action[AnyContent] = Action.andThen[AuthenticatedRequest](authenticateUser).async { implicit request =>
    sessionRepo.findSession.map {
      case Some(sessionData) => Ok(views.fakeReturnPage(Json.toJson(sessionData)))
      case None              => throw NoSessionException
    }

  }

}
