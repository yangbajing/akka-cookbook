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

package cookbook.actor.stopped

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorSystem, Behavior, PostStop, Props, SpawnProtocol }
import akka.util.Timeout
import helloscala.common.util.ActorUtils
import org.slf4j.Logger

import scala.concurrent.Await
import scala.concurrent.duration._

object Greet {
  sealed trait Command
  case object Stop extends Command
  case object GracefulStop extends Command
  def apply(): Behavior[Command] =
    Behaviors
      .receive[Command] {
        // #stopped
        case (context, Stop) =>
          context.log.info(s"Receive message: $Stop, will stopped.")
          Behaviors.stopped
        // #stopped
        // #stopped-cleanup
        case (context, GracefulStop) =>
          context.log.info(s"Receive message: $GracefulStop, will stopped.")
          Behaviors.stopped(() => cleanup(context.log))
        // #stopped-cleanup
      }
      .receiveSignal {
        case (context, PostStop) =>
          context.log.info(s"Receive signal: $PostStop.")
          Behaviors.same
      }

  // #cleanup
  def cleanup(log: Logger): Unit = {
    log.info("Perform cleanup action.")
  }
  // #cleanup
}

object StoppedMain {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(SpawnProtocol(), "spawn")
    implicit val timeout = Timeout(3.seconds)
    ActorUtils.spawnActor(Greet(), "stopped", Props.empty) ! Greet.Stop
    ActorUtils.spawnActor(Greet(), "graceful-stopped", Props.empty) ! Greet.GracefulStop
    TimeUnit.SECONDS.sleep(2)
    system.terminate()
    Await.ready(system.whenTerminated, 3.seconds)
  }
}
