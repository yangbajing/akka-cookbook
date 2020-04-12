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
import akka.actor.typed.delivery.ConsumerController
import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, Behavior }

import scala.concurrent.Future
import scala.util.{ Failure, Success }

// #TodoList
trait DB {
  def save(id: String, value: TodoList.State): Future[Done]
  def load(id: String): Future[TodoList.State]
}

object TodoList {
  sealed trait Command

  final case class AddTask(item: String) extends Command
  final case class CompleteTask(item: String) extends Command

  private final case class InitialState(state: State) extends Command
  private final case class SaveSuccess(confirmTo: ActorRef[ConsumerController.Confirmed]) extends Command
  private final case class DBError(cause: Throwable) extends Command

  private final case class CommandDelivery(command: Command, confirmTo: ActorRef[ConsumerController.Confirmed])
      extends Command

  final case class State(tasks: Vector[String])

  def apply(id: String, db: DB, consumerController: ActorRef[ConsumerController.Start[Command]]): Behavior[Command] = {
    Behaviors.setup[Command] { context =>
      new TodoList(context, id, db).start(consumerController)
    }
  }
}

class TodoList(context: ActorContext[TodoList.Command], id: String, db: DB) {
  import TodoList._

  private def start(consumerController: ActorRef[ConsumerController.Start[Command]]): Behavior[Command] = {
    context.pipeToSelf(db.load(id)) {
      case Success(value) => InitialState(value)
      case Failure(cause) => DBError(cause)
    }

    Behaviors.receiveMessagePartial {
      case InitialState(state) =>
        val deliveryAdapter: ActorRef[ConsumerController.Delivery[Command]] = context.messageAdapter { delivery =>
          CommandDelivery(delivery.message, delivery.confirmTo)
        }
        consumerController ! ConsumerController.Start(deliveryAdapter)
        active(state)
      case DBError(cause) =>
        throw cause
    }
  }

  private def active(state: State): Behavior[Command] = {
    Behaviors.receiveMessagePartial {
      case CommandDelivery(AddTask(item), confirmTo) =>
        val newState = state.copy(tasks = state.tasks :+ item)
        save(newState, confirmTo)
        active(newState)
      case CommandDelivery(CompleteTask(item), confirmTo) =>
        val newState = state.copy(tasks = state.tasks.filterNot(_ == item))
        save(newState, confirmTo)
        active(newState)
      case SaveSuccess(confirmTo) =>
        confirmTo ! ConsumerController.Confirmed
        Behaviors.same
      case DBError(cause) =>
        throw cause
    }
  }

  private def save(newState: State, confirmTo: ActorRef[ConsumerController.Confirmed]): Unit = {
    context.pipeToSelf(db.save(id, newState)) {
      case Success(_)     => SaveSuccess(confirmTo)
      case Failure(cause) => DBError(cause)
    }
  }
}
// #TodoList
