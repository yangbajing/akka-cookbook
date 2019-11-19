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

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed._
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

// #root-actor
object RootActor {
  sealed trait Command
  case class Spawn[T](behavior: Behavior[T], name: String, props: Props, replyTo: ActorRef[ActorRef[T]]) extends Command

  def apply(): Behavior[Command] = Behaviors.receive {
    case (context, Spawn(behavior, name, props, replyTo)) =>
      replyTo ! context.spawn(behavior, name, props)
      Behaviors.same
  }
}
// #root-actor

object CustomSpawnMain {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(RootActor(), "root-actor")
    implicit val timeout = Timeout(2.seconds)
    val pingF = system.ask[ActorRef[Ping.Command]](replyTo => RootActor.Spawn(Ping(), "ping", Props.empty, replyTo))
    val ping = Await.result(pingF, 2.seconds)
    ping ! Ping.Start
    system.terminate()
  }
}
