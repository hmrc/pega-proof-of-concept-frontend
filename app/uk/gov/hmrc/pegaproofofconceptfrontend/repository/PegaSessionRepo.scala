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

package uk.gov.hmrc.pegaproofofconceptfrontend.repository

import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}
import play.api.mvc.Request
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.cache.CacheIdType.SessionCacheId.NoSessionException
import uk.gov.hmrc.pegaproofofconceptfrontend.models.{SessionData, SessionId}
import uk.gov.hmrc.pegaproofofconceptfrontend.repository.PegaSessionRepo._
import uk.gov.hmrc.pegaproofofconceptfrontend.repository.Repo.{Id, IdExtractor}

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

final class PegaSessionRepo @Inject() (mongoComponent: MongoComponent, config: PegaRepoConfig)(implicit ec: ExecutionContext)
  extends Repo[SessionId, SessionData](
    collectionName = "pega-proof-of-concept",
    mongoComponent = mongoComponent,
    indexes        = indexes(config.expireMongo.toSeconds),
    replaceIndexes = true
  ) {

  def findSession(implicit request: Request[_]): Future[Option[SessionData]] = collection
    .find(
      filter = Filters.eq("sessionId", toSessionId(request).value)
    )
    .headOption()

}

object PegaSessionRepo {

  implicit val id: Id[SessionId] = new Id[SessionId] {
    override def value(i: SessionId): String = i.value
  }

  implicit val idExtractor: IdExtractor[SessionData, SessionId] = new IdExtractor[SessionData, SessionId] {
    override def id(a: SessionData): SessionId = a.sessionId
  }

  def toSessionId(request: Request[_]): SessionId = SessionId(request.session.get("sessionId").getOrElse(throw NoSessionException))

  def indexes(ttl: Long): Seq[IndexModel] = Seq(
    IndexModel(
      keys         = Indexes.ascending("sessionId"),
      indexOptions = IndexOptions().expireAfter(ttl, TimeUnit.SECONDS)
    )
  )
}
