/*
 * Copyright 2019 yangbajing.me
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

package helloscala.common.exception

import helloscala.common.IntStatus

case class HSHttpStatusException(
    override val msg: String,
    override val data: AnyRef = null,
    override val status: Int = IntStatus.BAD_REQUEST,
    override val httpStatus: Int = IntStatus.BAD_REQUEST,
    override val cause: Throwable = null)
    extends HSException(status, msg, cause)

case class HSAcceptedWarningException(
    override val msg: String,
    override val data: AnyRef = null,
    override val status: Int = IntStatus.ACCEPTED,
    override val cause: Throwable = null)
    extends HSException(status, msg, cause) {
  override val httpStatus: Int = IntStatus.ACCEPTED
}

case class HSBadRequestException(
    override val msg: String,
    override val data: AnyRef = null,
    override val status: Int = IntStatus.BAD_REQUEST,
    override val cause: Throwable = null)
    extends HSException(status, msg, cause) {
  override val httpStatus: Int = IntStatus.BAD_REQUEST
}

case class HSUnauthorizedException(
    override val msg: String,
    override val data: AnyRef = null,
    override val status: Int = IntStatus.UNAUTHORIZED,
    override val cause: Throwable = null)
    extends HSException(status, msg, cause) {
  override val httpStatus: Int = IntStatus.UNAUTHORIZED
}

case class HSNoContentException(
    override val msg: String,
    override val data: AnyRef = null,
    override val status: Int = IntStatus.NO_CONTENT,
    override val cause: Throwable = null)
    extends HSException(status, msg, cause) {
  override val httpStatus: Int = IntStatus.NO_CONTENT
}

case class HSForbiddenException(
    override val msg: String,
    override val data: AnyRef = null,
    override val status: Int = IntStatus.FORBIDDEN,
    override val cause: Throwable = null)
    extends HSException(status, msg, cause) {
  override val httpStatus: Int = IntStatus.FORBIDDEN
}

case class HSNotFoundException(
    override val msg: String,
    override val data: AnyRef = null,
    override val status: Int = IntStatus.NOT_FOUND,
    override val cause: Throwable = null)
    extends HSException(status, msg, cause) {
  override val httpStatus: Int = IntStatus.NOT_FOUND
}

case class HSConfigurationException(
    override val msg: String,
    override val data: AnyRef = null,
    override val status: Int = IntStatus.NOT_FOUND_CONFIG,
    override val cause: Throwable = null)
    extends HSException(status, msg, cause) {
  override val httpStatus: Int = IntStatus.NOT_FOUND
}

case class HSConflictException(
    override val msg: String,
    override val data: AnyRef = null,
    override val status: Int = IntStatus.CONFLICT,
    override val cause: Throwable = null)
    extends HSException(status, msg, cause) {
  override val httpStatus: Int = IntStatus.CONFLICT
}

case class HSNotImplementedException(
    override val msg: String,
    override val data: AnyRef = null,
    override val status: Int = IntStatus.NOT_IMPLEMENTED,
    override val cause: Throwable = null)
    extends HSException(status, msg, cause) {
  override val httpStatus: Int = IntStatus.NOT_IMPLEMENTED
}

case class HSInternalErrorException(
    override val msg: String,
    override val data: AnyRef = null,
    override val status: Int = IntStatus.INTERNAL_ERROR,
    override val cause: Throwable = null)
    extends HSException(status, msg, cause) {
  override val httpStatus: Int = IntStatus.INTERNAL_ERROR
}

case class HSBadGatewayException(
    override val msg: String,
    override val data: AnyRef = null,
    override val status: Int = IntStatus.BAD_GATEWAY,
    override val cause: Throwable = null)
    extends HSException(status, msg, cause) {
  override val httpStatus: Int = IntStatus.BAD_GATEWAY
}

case class HSServiceUnavailableException(
    override val msg: String,
    override val data: AnyRef = null,
    override val status: Int = IntStatus.SERVICE_UNAVAILABLE,
    override val cause: Throwable = null)
    extends HSException(status, msg, cause) {
  override val httpStatus: Int = IntStatus.SERVICE_UNAVAILABLE
}

case class HSGatewayTimeoutException(
    override val msg: String,
    override val data: AnyRef = null,
    override val status: Int = IntStatus.GATEWAY_TIMEOUT,
    override val cause: Throwable = null)
    extends HSException(status, msg, cause) {
  override val httpStatus: Int = IntStatus.GATEWAY_TIMEOUT
}
