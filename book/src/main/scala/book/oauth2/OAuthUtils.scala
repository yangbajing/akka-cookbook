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

package book.oauth2

import java.util.UUID

import helloscala.common.util.StringUtils

import scala.concurrent.duration.Duration

object OAuthUtils {
  type DueEpochMillis = Long

  // 过期时间比实际时间多5秒，保证客户端在过期时间点时刷新时新、旧 access_token 在一定时间内都有效
  val DEVIATION = 5000L

  def expiresInToEpochMillis(d: Duration): DueEpochMillis = System.currentTimeMillis() + d.toMillis + DEVIATION

  def generateToken(): String = {
    val iter = Iterator.continually(StringUtils.randomString(2))
    UUID.randomUUID().toString.split('-').zip(iter).map { case (x, y) => x + y }.mkString
  }

  def generateToken(userId: String): String = {
    userId + "-" + UUID.randomUUID().toString.replaceAll("-", "")
  }
}
