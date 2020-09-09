package cookbook.actor.dispatcher

import java.util.concurrent.ThreadPoolExecutor

import akka.actor.{ActorCell, Cell}
import akka.dispatch.sysmsg.SystemMessage
import akka.dispatch.{Dispatcher, Envelope, ExecutorServiceFactoryProvider, Mailbox, MailboxType, MessageDispatcher, MessageDispatcherConfigurator, TaskInvocation}

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
