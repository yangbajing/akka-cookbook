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
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }

import scala.collection.mutable

// #FiniteStateMachine
object FiniteStateMachine {
  def apply(): Behavior[String] = Behaviors.setup { context =>
    new FiniteStateMachine(context).idle()
  }
}

class FiniteStateMachine private (context: ActorContext[String]) {
  private val pendingMessages = mutable.Queue.empty[String]

  def idle(): Behavior[String] = Behaviors.receiveMessage {
    case "running" =>
      active()
    case msg =>
      context.log.info(s"[idle] receive message: $msg")
      pendingMessages enqueue msg
      Behaviors.same
  }

  def active(): Behavior[String] = Behaviors.receiveMessage {
    case "passive" =>
      idle()
    case msg =>
      context.log.info(s"[active] receive message: $msg")
      processPendingMessages()
      Behaviors.same
  }

  private def processPendingMessages(): Unit = {
    while (pendingMessages.nonEmpty) {
      val msg = pendingMessages.dequeue()
      context.log.info(s"Process pending message: $msg")
    }
  }
}
// #FiniteStateMachine
