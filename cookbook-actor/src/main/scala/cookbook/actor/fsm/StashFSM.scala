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

package cookbook.actor.fsm

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, StashBuffer }

// #StashFSM
object StashFSM {
  def apply(): Behavior[String] = Behaviors.setup { context =>
    Behaviors.withStash(1024)(stash => new StashFSM(stash, context).idle())
  }
}
class StashFSM private (stash: StashBuffer[String], context: ActorContext[String]) {
  def idle(): Behavior[String] = Behaviors.receiveMessage {
    case "running" =>
      stash.unstashAll(active())
    case msg =>
      context.log.info(s"[idle] receive message: $msg")
      stash.stash(msg)
      Behaviors.same
  }

  def active(): Behavior[String] = Behaviors.receiveMessage {
    case "passive" =>
      idle()
    case msg =>
      context.log.info(s"[active] receive message: $msg")
      Behaviors.same
  }
}
// #StashFSM
