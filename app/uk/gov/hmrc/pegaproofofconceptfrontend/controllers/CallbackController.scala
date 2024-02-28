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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.mongo.cache.CacheIdType.SessionCacheId.NoSessionException
import uk.gov.hmrc.pegaproofofconceptfrontend.connectors.PegaProxyConnector
import uk.gov.hmrc.pegaproofofconceptfrontend.controllers.actions.{AuthenticatedAction, AuthenticatedRequest}
import uk.gov.hmrc.pegaproofofconceptfrontend.repository.PegaSessionRepo
import uk.gov.hmrc.pegaproofofconceptfrontend.views.Views
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CallbackController @Inject() (
    mcc:              MessagesControllerComponents,
    authenticateUser: AuthenticatedAction,
    sessionRepo:      PegaSessionRepo,
    connector:        PegaProxyConnector,
    views:            Views
)(implicit ec: ExecutionContext)
  extends FrontendController(mcc) with Logging with I18nSupport {

  def callback(p: String): Action[AnyContent] = Action.andThen[AuthenticatedRequest](authenticateUser).async { implicit request =>
    logger.info(s"query parameter was $p")
    sessionRepo.findSession.flatMap {
      case Some(sessionData) =>
        if (sessionData.pegaJourneyResponse.ID === p)
          connector.getCase(p).flatMap {
            case response if response.status === OK =>
              sessionRepo.upsert(sessionData.copy(getCaseResponse = Some(response.json))).map(_ =>
                Redirect(routes.CallbackController.returns))
            case other =>
              sys.error(s"call to get case came back with ${other.status.toString}")
          }
        else
          sys.error(s"query parameter $p did not match caseId in mongo, ${sessionData.pegaJourneyResponse.ID}")
      case None => throw NoSessionException
    }
  }

  val returns: Action[AnyContent] = Action.andThen[AuthenticatedRequest](authenticateUser).async { implicit request =>
    sessionRepo.findSession.map {
      case Some(sessionData) => Ok(views.fakeReturnPage(Json.toJson(sessionData)))
      case None              => throw NoSessionException
    }

  }

}
