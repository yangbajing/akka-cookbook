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

package cookbook.serialization.jackson

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.serialization.jackson.JacksonObjectMapperProvider
import akka.actor.typed.scaladsl.adapter._
import org.scalatest.WordSpecLike

object SerializationJacksonTest {
  case class User(name: String, sex: Int)
  case class Error(status: Int, message: String)
}
class SerializationJacksonTest extends ScalaTestWithActorTestKit with WordSpecLike {
  import SerializationJacksonTest._
  private lazy val objectMapper = JacksonObjectMapperProvider(system.toClassic).getOrCreate("jackson-json", None)
  "serialization-jackson" should {
    "either" in {
      val right: Either[Error, User] = Right(User("羊八井", 1))
      val rightText = objectMapper.writeValueAsString(right)
      println(rightText)
      rightText should be("""{"r":{"name":"羊八井","sex":1}}""")
      objectMapper.readValue(rightText, classOf[Right[Error, User]]) should be(right)

      val left: Either[Error, User] = Left(Error(400, "Bad Request."))
      val leftText = objectMapper.writeValueAsString(left)
      println(leftText)
      leftText should be("""{"l":{"status":400,"message":"Bad Request."}}""")
      objectMapper.treeToValue(objectMapper.readTree(leftText), classOf[Either[Error, User]]) should be(left)
    }
  }
}
