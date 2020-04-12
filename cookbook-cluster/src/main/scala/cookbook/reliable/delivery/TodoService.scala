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

package cookbook.reliable.delivery

import akka.Done
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, Behavior }
import akka.cluster.sharding.typed.delivery.ShardingProducerController
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{ Failure, Success }

// #TodoService
object TodoService {
  sealed trait Command

  final case class UpdateTodo(listId: String, item: String, completed: Boolean, replyTo: ActorRef[Response])
      extends Command

  sealed trait Response
  case object Accepted extends Response
  case object Rejected extends Response
  case object MaybeAccepted extends Response

  private final case class WrappedRequestNext(requestNext: ShardingProducerController.RequestNext[TodoList.Command])
      extends Command
  private final case class Confirmed(originalReplyTo: ActorRef[Response]) extends Command
  private final case class TimedOut(originalReplyTo: ActorRef[Response]) extends Command

  def apply(producerController: ActorRef[ShardingProducerController.Command[TodoList.Command]]): Behavior[Command] = {
    Behaviors.setup { context =>
      new TodoService(context).start(producerController)
    }
  }
}

class TodoService(context: ActorContext[TodoService.Command]) {
  import TodoService._

  private implicit val askTimeout: Timeout = 5.seconds

  private def start(
      producerController: ActorRef[ShardingProducerController.Start[TodoList.Command]]): Behavior[Command] = {
    val requestNextAdapter: ActorRef[ShardingProducerController.RequestNext[TodoList.Command]] =
      context.messageAdapter(WrappedRequestNext.apply)
    producerController ! ShardingProducerController.Start(requestNextAdapter)

    Behaviors.receiveMessagePartial {
      case WrappedRequestNext(next) =>
        active(next)
      case UpdateTodo(_, _, _, replyTo) =>
        // not hooked up with shardingProducerController yet
        replyTo ! Rejected
        Behaviors.same
    }
  }

  private def active(requestNext: ShardingProducerController.RequestNext[TodoList.Command]): Behavior[Command] = {
    Behaviors.receiveMessage {
      case WrappedRequestNext(next) =>
        active(next)

      case UpdateTodo(listId, item, completed, replyTo) =>
        if (requestNext.bufferedForEntitiesWithoutDemand.getOrElse(listId, 0) >= 100)
          replyTo ! Rejected
        else {
          val requestMsg = if (completed) TodoList.CompleteTask(item) else TodoList.AddTask(item)
          context.ask[ShardingProducerController.MessageWithConfirmation[TodoList.Command], Done](
            requestNext.askNextTo,
            askReplyTo => ShardingProducerController.MessageWithConfirmation(listId, requestMsg, askReplyTo)) {
            case Success(Done) => Confirmed(replyTo)
            case Failure(_)    => TimedOut(replyTo)
          }
        }
        Behaviors.same

      case Confirmed(originalReplyTo) =>
        originalReplyTo ! Accepted
        Behaviors.same

      case TimedOut(originalReplyTo) =>
        originalReplyTo ! MaybeAccepted
        Behaviors.same
    }
  }
}
// #TodoService
