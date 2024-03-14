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

package uk.gov.hmrc.pegaproofofconceptfrontend.models

import cats.Eq
import play.api.libs.json.{Format, Json}
import play.api.mvc.QueryStringBindable

import java.net.URLEncoder

final case class CaseId(value: String)

object CaseId {

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val format: Format[CaseId] = Json.valueFormat[CaseId]

  implicit val queryStringBindable: QueryStringBindable[CaseId] = new QueryStringBindable[CaseId] {
    def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, CaseId]] =
      params.get(key).flatMap(_.headOption.map(s => Right(CaseId(s))))

    def unbind(key: String, value: CaseId) = s"$key=${URLEncoder.encode(value.value, "UTF-8")}"
  }

  implicit val eq: Eq[CaseId] = Eq.fromUniversalEquals

}
