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

package cookbook.actor.introduction

import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.actor.typed.scaladsl.Behaviors

// #ping-pong
object Ping {
  // #ping
  sealed trait Command
  final case object Start extends Command
  final case class PongCommand(message: String) extends Command
  def apply(): Behavior[Command] = Behaviors.receive {
    case (context, Start) =>
      val pong = context.spawn(Pong(), "pong")
      pong ! Pong.PingCommand("Scala", context.self)
      context.log.info("Started Pong actor and send message complete.")
      Behaviors.same
    case (context, PongCommand(message)) =>
      context.log.info(s"Receive pong message: $message")
      Behaviors.stopped
  }
  // #ping
}

object Pong {
  sealed trait Command
  final case class PingCommand(message: String, replyTo: ActorRef[Ping.Command])
      extends Command
  def apply(): Behavior[Command] = Behaviors.receive[Command] {
    case (context, PingCommand(message, replyTo)) =>
      context.log.info(s"Receive ping message: $message")
      replyTo ! Ping.PongCommand(s"Hello $message")
      Behaviors.stopped
  }
}
// #ping-pong

object HelloWorld {
  def main(args: Array[String]): Unit = {
    // #helloworld
    val system: ActorSystem[Ping.Command] = ActorSystem(Ping(), "helloworld")
    system ! Ping.Start
    system.terminate()
    // #helloworld
  }
}
