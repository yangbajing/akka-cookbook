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

package cookbook.rate

import java.lang.Math.min
import java.util.concurrent.TimeUnit.SECONDS

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }

import scala.concurrent.duration.Duration

/**
 * 基于 Akka 实现的集群限流
 * @author Yang Jing <a href="mailto:yang.xunjing@qq.com">yangbajing</a>
 * @date 2020-11-11 19:26:40
 */
object ClusterRateLimiter {
  sealed trait Command

  case class Acquire(n: Int, timeout: Duration, replyTo: ActorRef[Acquired]) extends Command
  case class Acquired(result: Boolean)
  case class SetRate(permitsPerSecond: Double) extends Command

  val entityTypeKey: EntityTypeKey[Command] = EntityTypeKey("ClusterRateLimiter")

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] =
    ClusterSharding(system).init(Entity(entityTypeKey)(entityContext => apply(entityContext.entityId)))

  private[rate] def apply(name: String): Behavior[Command] = Behaviors.setup { context =>
    // TODO
    val maxBurstSeconds = .0

    new ClusterRateLimiter(name, maxBurstSeconds, context).receive
  }
}

import cookbook.rate.ClusterRateLimiter._
class ClusterRateLimiter(name: String, maxBurstSeconds: Double, context: ActorContext[Command]) {
  private val config = context.system.settings.config

  /** The currently stored permits. */
  private var storedPermits: Double = .0

  /** The maximum number of stored permits. */
  private var maxPermits: Double = .0

  /**
   * The interval between two unit requests, at our stable rate. E.g., a stable rate of 5 permits
   * per second has a stable interval of 200ms.
   */
  private var stableIntervalMicros: Double = .0

  /**
   * The time when the next request (no matter its size) will be granted. After granting a request,
   * this is pushed further in the future. Large requests push this further than small requests.
   */
  private var nextFreeTicketMicros: Long = 0L // could be either in the past or future

  def receive: Behavior[Command] = Behaviors.receiveMessage {
    case Acquire(n, timeout, replyTo) =>
      Behaviors.same
      val result = tryAcquire(n, timeout)
      replyTo ! Acquired(result)
      Behaviors.same
    case SetRate(permitsPerSecond) =>
      setRate(permitsPerSecond)
      Behaviors.same
  }

  def tryAcquire(n: Int, timeout: Duration): Boolean = ???

  def setRate(permitsPerSecond: Double): Unit = {
    doSetRate(permitsPerSecond, System.nanoTime() / 1000)
  }

  private def doSetRate(permitsPerSecond: Double, nowMicros: Long): Unit = {
    resync(nowMicros)
    val stableIntervalMicros = SECONDS.toMicros(1L) / permitsPerSecond
    this.stableIntervalMicros = stableIntervalMicros
    doSetRate(permitsPerSecond, stableIntervalMicros)
  }

  private def doSetRate(permitsPerSecond: Double, stableIntervalMicros: Double): Unit = {
    val oldMaxPermits = this.maxPermits
    maxPermits = maxBurstSeconds * permitsPerSecond
    storedPermits =
      if (oldMaxPermits == Double.PositiveInfinity) { // if we don't special-case this, we would get storedPermits == NaN, below
        maxPermits
      } else if (oldMaxPermits == 0.0) { // initial state
        0.0
      } else {
        storedPermits * maxPermits / oldMaxPermits
      }
  }

  private def resync(nowMicros: Long): Unit = { // if nextFreeTicket is in the past, resync to now
    if (nowMicros > nextFreeTicketMicros) {
      val newPermits = (nowMicros - nextFreeTicketMicros) / stableIntervalMicros
      storedPermits = min(maxPermits, storedPermits + newPermits)
      nextFreeTicketMicros = nowMicros
    }
  }
}
