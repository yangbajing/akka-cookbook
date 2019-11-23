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

package helloscala.common.util

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed._
import akka.util.Timeout

import scala.concurrent.Await

object ActorUtils {
  def spawnActor[T](
      system: ActorSystem[SpawnProtocol.Command],
      behavior: Behavior[T],
      name: String,
      props: Props)(implicit timeout: Timeout): ActorRef[T] =
    spawnActor(behavior, name, props)(system, timeout, system.scheduler)

  def spawnActor[T](behavior: Behavior[T], name: String, props: Props)(
      implicit recipient: RecipientRef[SpawnProtocol.Command],
      timeout: Timeout,
      scheduler: Scheduler): ActorRef[T] = {
    val future = recipient.ask[ActorRef[T]](replyTo =>
      SpawnProtocol.Spawn(behavior, name, props, replyTo))
    Await.result(future, timeout.duration)
  }
}
