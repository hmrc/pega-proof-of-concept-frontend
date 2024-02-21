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

package uk.gov.hmrc.pegaproofofconceptfrontend.repository

import org.mongodb.scala.model.{Filters, IndexModel, ReplaceOptions}
import play.api.libs.json._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.pegaproofofconceptfrontend.repository.Repo.{Id, IdExtractor}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

@SuppressWarnings(Array("org.wartremover.warts.Any"))
abstract class Repo[ID, A: ClassTag](
    collectionName: String,
    mongoComponent: MongoComponent,
    indexes:        Seq[IndexModel],
    replaceIndexes: Boolean         = false
)(implicit
    domainFormat: OFormat[A],
  executionContext: ExecutionContext,
  id:               Id[ID],
  idExtractor:      IdExtractor[A, ID]
)
  extends PlayMongoRepository[A](
    mongoComponent = mongoComponent,
    collectionName = collectionName,
    domainFormat   = domainFormat,
    indexes        = indexes,
    replaceIndexes = replaceIndexes
  ) {

  /**
   * Update or Insert (UpSert)
   */
  def upsert(a: A): Future[Unit] = collection
    .replaceOne(
      filter      = Filters.eq("_id", id.value(idExtractor.id(a))),
      replacement = a,
      options     = ReplaceOptions().upsert(true)
    )
    .toFuture()
    .map(_ => ())
}

object Repo {

  trait Id[I] {
    def value(i: I): String
  }

  trait IdExtractor[A, ID] {
    def id(a: A): ID
  }

}
