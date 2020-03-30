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

import fusion.json.CborSerializable

/**
 * @param access_token 访问令牌
 * @param expires_in 令牌有效期（秒）
 * @param refresh_token 刷新令牌
 */
case class AccessToken(access_token: String, expires_in: Long, refresh_token: String) extends CborSerializable
