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

package cookbook.actor.fault

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed._
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.Await
import scala.concurrent.duration._

object FaultTolerance extends StrictLogging {
  // #FaultTolerance
  case class RestartException(message: String) extends RuntimeException(message)
  case class StopException(message: String) extends RuntimeException(message)

  sealed trait Command
  final case object Message extends Command
  final case object Restart extends Command
  final case object Stop extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    println(s"${context.self} started.")
    Behaviors
      .receiveMessage[Command] {
        case Message =>
          println(s"${context.self} Received Message.")
          Behaviors.same
        case Restart =>
          throw RestartException("可重启")
        case Stop =>
          throw StopException("退出")
      }
      .receiveSignal {
        case (context, PreRestart) =>
          println(s"${context.self} Received signal $PreRestart")
          Behaviors.same
        case (context, PostStop) =>
          println(s"${context.self} Received signal $PostStop")
          Behaviors.same
      }
  }
  // #FaultTolerance

  def main(args: Array[String]): Unit = {
    val system = ActorSystem(
      Behaviors
        .supervise(
          Behaviors
            .supervise(FaultTolerance())
            .onFailure[RestartException](SupervisorStrategy.restart))
        .onFailure[StopException](SupervisorStrategy.stop),
      "fault-tolerance")
    system ! Message
    system ! Restart
    system ! Stop
    system.terminate()
    Await.ready(system.whenTerminated, 10.seconds)
  }
}
