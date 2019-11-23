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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import helloscala.common.IntStatus

@JsonIgnoreProperties(
  value = Array("suppressed", "localizedMessage", "stackTrace", "cause"))
class HSException(
    val status: Int,
    val msg: String,
    val cause: Throwable,
    enableSuppression: Boolean,
    writableStackTrace: Boolean)
    extends RuntimeException(msg, cause, enableSuppression, writableStackTrace) {
  def httpStatus: Int = IntStatus.INTERNAL_ERROR

  val data: Object = null

  def this(status: Int) {
    this(status, null, null, true, false)
  }

  def this(status: Int, message: String) {
    this(status, message, null, true, false)
  }

  def this(status: Int, message: String, cause: Throwable) {
    this(status, message, cause, true, false)
  }

  def this(message: String) {
    this(IntStatus.INTERNAL_ERROR, message, null, true, false)
  }

  def this(message: String, cause: Throwable) {
    this(IntStatus.INTERNAL_ERROR, message, cause, true, false)
  }

  def getStatus(): Int = status
}
