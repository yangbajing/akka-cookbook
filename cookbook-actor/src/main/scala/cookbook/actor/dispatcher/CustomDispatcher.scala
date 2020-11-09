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

package cookbook.actor.dispatcher

import java.util.concurrent.ThreadPoolExecutor

import akka.actor.{ ActorCell, Cell }
import akka.dispatch.sysmsg.SystemMessage
import akka.dispatch.{
  Dispatcher,
  Envelope,
  ExecutorServiceFactoryProvider,
  Mailbox,
  MailboxType,
  MessageDispatcher,
  MessageDispatcherConfigurator,
  TaskInvocation
}

import scala.concurrent.duration._

class CustomDispatcher(
    _configurator: MessageDispatcherConfigurator,
    override val id: String,
    override val throughput: Int,
    override val throughputDeadlineTime: Duration,
    executorServiceFactoryProvider: ExecutorServiceFactoryProvider,
    override val shutdownTimeout: FiniteDuration)
    extends Dispatcher(
      _configurator,
      id,
      throughput,
      throughputDeadlineTime,
      executorServiceFactoryProvider,
      shutdownTimeout) {
  val pool = executorService.executor.asInstanceOf[ThreadPoolExecutor]
}
