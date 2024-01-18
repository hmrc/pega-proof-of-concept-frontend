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

package uk.gov.hmrc.pegaproofofconceptfrontend.config

import com.google.inject.Inject
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

final case class ViewConfig(
    appName:                String,
    authUrl:                String,
    companyAuthUrl:         String,
    signInPath:             String,
    frontendBaseUrl:        String,
    timeoutDialogTimeout:   Int,
    timeoutDialogCountdown: Int
) {

  @Inject
  def this(servicesConfig: ServicesConfig) = this(
    appName                = servicesConfig.getString("appName"),
    authUrl                = servicesConfig.baseUrl("auth"),
    companyAuthUrl         = servicesConfig.getString("microservice.services.company-auth.url"),
    signInPath             = servicesConfig.getString("microservice.services.company-auth.sign-in-path"),
    frontendBaseUrl        = servicesConfig.getString("microservice.services.auth.login-callback.base-url"),
    timeoutDialogTimeout   = servicesConfig.getInt("timeout-dialog.timeout"),
    timeoutDialogCountdown = servicesConfig.getInt("timeout-dialog.countdown")
  )

}
