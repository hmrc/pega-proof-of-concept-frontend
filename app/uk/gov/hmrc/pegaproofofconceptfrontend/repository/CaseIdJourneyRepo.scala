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
import play.api.Configuration
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.pegaproofofconceptfrontend.models.{CaseId, SessionData}
import uk.gov.hmrc.pegaproofofconceptfrontend.repository.CaseIdJourneyRepo.{SessionDataWithCaseId, id, indexes}
import uk.gov.hmrc.pegaproofofconceptfrontend.repository.Repo.{Id, IdExtractor}

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CaseIdJourneyRepo @Inject() (mongoComponent: MongoComponent, config: Configuration)(implicit ec: ExecutionContext)
  extends Repo[CaseId, SessionDataWithCaseId](
    collectionName = "case-id-journey-repo",
    mongoComponent = mongoComponent,
    indexes        = indexes(config.get[FiniteDuration]("mongodb.case-id-journey-ttl").toSeconds),
    replaceIndexes = true
  ) {

  def findSession(caseId: CaseId): Future[Option[SessionData]] =
    collection
      .find(
        filter = Filters.eq("caseId", caseId.value)
      )
      .headOption()
      .map(_.map(_.sessionData))

  def insertSession(caseId: CaseId, sessionData: SessionData): Future[Unit] =
    collection
      .insertOne(SessionDataWithCaseId(caseId, sessionData))
      .toFuture()
      .map(result => if (result.wasAcknowledged()) () else sys.error("insert into mongo was not acknowledged"))

}

object CaseIdJourneyRepo {

  final case class SessionDataWithCaseId(caseId: CaseId, sessionData: SessionData)

  object SessionDataWithCaseId {
    @SuppressWarnings(Array("org.wartremover.warts.Any"))
    implicit val format: OFormat[SessionDataWithCaseId] = Json.format
  }

  implicit val id: Id[CaseId] = new Id[CaseId] {
    override def value(i: CaseId): String = i.value
  }

  implicit val idExtractor: IdExtractor[SessionDataWithCaseId, CaseId] = new IdExtractor[SessionDataWithCaseId, CaseId] {
    override def id(a: SessionDataWithCaseId): CaseId = a.caseId
  }

  def indexes(ttl: Long): Seq[IndexModel] = Seq(
    IndexModel(
      keys         = Indexes.ascending("caseId"),
      indexOptions = IndexOptions().expireAfter(ttl, TimeUnit.SECONDS)
    )
  )
}
