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

package cookbook.actor.style

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors, TimerScheduler }
import akka.actor.typed.{ ActorRef, Behavior, PostStop, PreRestart }

import scala.concurrent.Future
import scala.util.{ Failure, Success }

class DataSourceFactory {
  def createDataSource(): AutoCloseable = ???
}

// #Hello-object
object Hello {
  trait Command
  case object Start extends Command
  final case class Question(message: String, replyTo: ActorRef[Reply]) extends Command
  case object StopOrder extends Command

  trait Reply
  final case class Answer(message: String) extends Reply

  def apply(dataSourceFactory: DataSourceFactory): Behavior[Command] =
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new Hello(dataSourceFactory, context, timers).init()
      }
    }
}
// #Hello-object

// #Hello-class
class Hello private (
    dataSourceFactory: DataSourceFactory,
    context: ActorContext[Hello.Command],
    timers: TimerScheduler[Hello.Command]) {
  import Hello._
  import context.executionContext

  private var ds: AutoCloseable = _

  def init(): Behavior[Command] = {
    val future = Future {
      context.log.debug("开始异步初始化外部资源，idle……")
      ds = dataSourceFactory.createDataSource()
    }

    context.pipeToSelf(future) {
      case Success(_) => Start
      case Failure(exception) =>
        context.log.error(s"Init error: $exception")
        StopOrder
    }

    idle()
  }

  def idle(): Behavior[Command] = Behaviors.withStash(1024) { stash =>
    Behaviors.receiveMessage {
      case Start =>
        context.log.debug("外部资源初始化完成，active……")
        // 切换行为active前，回放所有已stash消息
        stash.unstashAll(active())
      case StopOrder =>
        if (stash.nonEmpty) {
          var messages = List.empty[Command]
          stash.foreach(messages ::= _)
          loggingStashedMessages(messages)
        }
        Behaviors.stopped
      case msg =>
        // 在 Start 前，stash进入的消息
        stash.stash(msg)
        Behaviors.same
    }
  }

  def active(): Behavior[Command] =
    Behaviors
      .receiveMessage[Command] {
        case Question(message, replyTo) =>
          replyTo ! Answer(s"You say is $message.")
          Behaviors.same

        // 其它业务 case
        // ...

        case StopOrder =>
          Behaviors.stopped
      }
      .receiveSignal {
        case (_, PreRestart) =>
          cleanup()
          Behaviors.same
        case (_, PostStop) =>
          cleanup()
          Behaviors.same
      }

  private def loggingStashedMessages(messages: List[Command]): Unit = {
    // 对未处理的stash消息记录日志
  }

  private def cleanup(): Unit = {
    if (null != ds) {
      ds.close()
    }
  }
}
// #Hello-class

object HelloFunction {
  import Hello._

  // #function
  private case object InternalInit extends Command
  def apply(dataSourceFactory: DataSourceFactory): Behavior[Command] = {
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        import context.executionContext
        context.self ! InternalInit

        def init(): Future[AutoCloseable] = Future {
          context.log.debug("开始异步初始化外部资源，idle……")
          dataSourceFactory.createDataSource()
        }

        def idle(): Behavior[Command] = Behaviors.withStash(1024) { stash =>
          var ds: AutoCloseable = null
          Behaviors.receiveMessage {
            case InternalInit =>
              context.pipeToSelf(init()) {
                case Success(value) =>
                  ds = value
                  Start
                case Failure(e) =>
                  context.log.error(s"Init error: $e")
                  StopOrder
              }
              Behaviors.same
            case Start =>
              context.log.debug("外部资源初始化完成，active……")
              // 切换行为active前，回放所有已stash消息
              stash.unstashAll(active(ds))
            case StopOrder =>
              if (stash.nonEmpty) {
                var messages = List.empty[Command]
                stash.foreach(messages ::= _)
                loggingStashedMessages(messages)
              }
              Behaviors.stopped
            case msg =>
              // 在 Start 前，stash进入的消息
              stash.stash(msg)
              Behaviors.same
          }
        }

        def active(ds: AutoCloseable): Behavior[Command] =
          Behaviors
            .receiveMessage[Command] {
              case Question(message, replyTo) =>
                replyTo ! Answer(s"You say is $message.")
                Behaviors.same

              // 其它业务 case
              // ...

              case StopOrder =>
                Behaviors.stopped
            }
            .receiveSignal {
              case (_, PreRestart) =>
                cleanup(ds)
                Behaviors.same
              case (_, PostStop) =>
                cleanup(ds)
                Behaviors.same
            }

        def loggingStashedMessages(messages: List[Command]): Unit = {
          // 对未处理的stash消息记录日志
        }

        def cleanup(ds: AutoCloseable): Unit = {
          if (null != ds) {
            ds.close()
          }
        }

        idle()
      }
    }
  }
  // #function
}
