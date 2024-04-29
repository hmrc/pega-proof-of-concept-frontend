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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.pegaproofofconceptfrontend.models.StartCaseResponse.Data

final case class StartCaseResponse(data: Data, ID: CaseId)

object StartCaseResponse {

  final case class Assignment(ID: AssignmentId)

  object Assignment {
    implicit val format: OFormat[Assignment] = Json.format
  }

  final case class CaseInfo(assignments: List[Assignment])

  object CaseInfo {
    implicit val format: OFormat[CaseInfo] = Json.format
  }

  final case class Data(caseInfo: CaseInfo)

  object Data {
    implicit val format: OFormat[Data] = Json.format
  }

  @SuppressWarnings(Array("org.wartremover.warts.Any"))
  implicit val format: OFormat[StartCaseResponse] = Json.format

}
