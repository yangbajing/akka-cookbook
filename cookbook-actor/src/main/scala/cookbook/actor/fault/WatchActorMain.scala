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

import java.util.concurrent.TimeUnit

import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._

// #WatchActorMain
final case class EscalateException(message: String) extends RuntimeException(message)
final case class ActorException(ref: ActorRef[Nothing], cause: Throwable) extends RuntimeException(cause)

// #child
object Child {
  sealed trait Command
  case object ThrowNormalException extends Command
  case object ThrowEscalateException extends Command
  def apply(): Behavior[Command] = Behaviors.setup { context =>
    context.log.info("started.")
    Behaviors
      .receiveMessage[Command] {
        case ThrowEscalateException =>
          throw EscalateException("This is escalate exception.")
        case ThrowNormalException =>
          throw new RuntimeException("This is normal exception.")
      }
      .receiveSignal {
        case (_, PreRestart) =>
          context.log.info("Pre restart.")
          Behaviors.same
        case (_, PostStop) =>
          context.log.info("stopped.")
          Behaviors.same
      }
  }
}
// #child

// #parent
object Parent {
  sealed trait Command
  def apply(): Behavior[Command] = Behaviors.setup { context =>
    import context.executionContext
    val child1 = context.spawn(Child(), "child1")
    context.watch(child1)
    val child2 = context.spawn(Child(), "child2")
    context.watch(child2)
    child2 ! Child.ThrowNormalException
    context.system.scheduler.scheduleOnce(1.second, () => child1 ! Child.ThrowEscalateException)
    Behaviors.receiveSignal {
      case (_, ChildFailed(ref, e: EscalateException)) =>
        throw ActorException(ref, e)
      case (_, ChildFailed(ref, e)) =>
        context.log.warn(s"Received child actor ${ref.path} terminated signal, original exception is $e")
        Behaviors.same
    }
  }
}
// #parent

// #root
object Root {
  sealed trait Command
  def apply(): Behavior[Command] = Behaviors.setup { context =>
    val parent = context.spawn(Parent(), "parent")
    context.watch(parent)
    Behaviors.receiveSignal {
      case (_, ChildFailed(ref, e)) =>
        context.log.info(s"Received child actor ${ref.path} failed signal, original exception is $e")
        Behaviors.same
      case (_, Terminated(ref)) =>
        context.log.info(s"Received actor ${ref.path} terminated signal.")
        Behaviors.same
    }
  }
}
// #root

object WatchActorMain {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem(Root(), "watch")
    TimeUnit.SECONDS.sleep(2)
    system.terminate()
  }
}
// #WatchActorMain
