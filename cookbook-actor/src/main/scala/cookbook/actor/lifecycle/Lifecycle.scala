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

package cookbook.actor.lifecycle

import akka.actor.typed.{ Behavior, PostStop, PreRestart }
import akka.actor.typed.scaladsl.Behaviors

// #Lifecycle
object Lifecycle {
  def apply(): Behavior[String] = Behaviors.setup { context =>
    context.log.info("actor started.")
    Behaviors
      .receiveMessage[String] {
        case "restart" =>
          context.log.info("Beginning restart.")
          throw new RuntimeException("Beginning restart.")
        case message =>
          context.log.info(s"Received message is $message")
          Behaviors.same
      }
      .receiveSignal {
        case (_, PreRestart) =>
          context.log.info("actor pre restart.")
          Behaviors.same
        case (_, PostStop) =>
          context.log.info("actor post stop.")
          Behaviors.same
      }
  }
}
// #Lifecycle
