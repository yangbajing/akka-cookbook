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

package cookbook.actor.requestresponse

import java.util.concurrent.TimeUnit

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior, Props, SpawnProtocol }
import akka.util.Timeout
import helloscala.common.util.ActorUtils

import scala.concurrent.duration._

object Request {
  // #command
  sealed trait Command
  private final case class WrappedBackendResponse(response: Backend.Response) extends Command
  // #command

  final case object Start extends Command

  def apply(): Behavior[Command] = Behaviors.setup[Command] { context =>
    // #adapted-message
    val backendAdapter =
      context.messageAdapter[Backend.Response](resp => WrappedBackendResponse(resp))
    // #adapted-message

    // #request-response
    val backend = context.spawn(Backend(), "backend")

    Behaviors.receiveMessage {
      case Start =>
        backend ! Backend.StartJob("Crawler news", backendAdapter)
        Behaviors.same
      case WrappedBackendResponse(response) =>
        response match {
          case Backend.JobStarted =>
            context.log.info("Send to backend job started.")
        }
        Behaviors.same
    }
    // #request-response
  }
}

object Backend {
  sealed trait Command
  final case class StartJob(job: String, replyTo: ActorRef[Response]) extends Command

  sealed trait Response
  case object JobStarted extends Response

  def apply(): Behavior[Command] = Behaviors.setup[Command] { context =>
    Behaviors.receiveMessage {
      case StartJob(job, replyTo) =>
        context.log.info(s"Start job [$job] success.")
        replyTo ! JobStarted
        Behaviors.same
    }
  }
}

object RequestResponse {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem(SpawnProtocol(), "request-response")
    implicit val timeout = Timeout(2.seconds)
    ActorUtils.spawnActor(system, Request(), "request", Props.empty) ! Request.Start
    TimeUnit.SECONDS.sleep(1)
    system.terminate()
  }
}
