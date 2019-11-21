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

package cookbook.actor.timer

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.duration._

// #Timer
object Timer {
  sealed trait Command
  case object ReceiveTimeout extends Command
  case object SingleTrigger extends Command
  case object TimerTrigger extends Command
  case object CancelAllTimer extends Command

  def apply(): Behavior[Command] =
    Behaviors.setup(context =>
      Behaviors.withTimers { timers =>
        context.setReceiveTimeout(2.seconds, ReceiveTimeout)
        timers.startSingleTimer(SingleTrigger, SingleTrigger, 2.seconds)
        timers.startTimerAtFixedRate(TimerTrigger, TimerTrigger, 1.seconds)

        Behaviors.receiveMessage {
          case SingleTrigger =>
            context.log.info(s"Receive message: $SingleTrigger")
            Behaviors.same
          case TimerTrigger =>
            context.log.info(s"Receive message: $TimerTrigger")
            Behaviors.same
          case CancelAllTimer =>
            context.log.info(s"Receive message: $CancelAllTimer")
            timers.cancelAll()
            Behaviors.same
          case ReceiveTimeout =>
            context.log.info(s"Receive message: $ReceiveTimeout")
            Behaviors.same
        }
      })
}
// #Timer
