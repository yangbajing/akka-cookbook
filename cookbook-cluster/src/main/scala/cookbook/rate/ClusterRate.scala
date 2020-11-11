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

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import com.google.common.util.concurrent.RateLimiter

/**
 * 基于 Akka 实现的集群限流，使用 guava
 * @author Yang Jing <a href="mailto:yang.xunjing@qq.com">yangbajing</a>
 * @date 2020-11-11 20:22:04
 */
// #ClusterRate
object ClusterRate {
  sealed trait Command

  case class Acquire(n: Int, replyTo: ActorRef[Acquired]) extends Command
  case class Acquired(result: Boolean)
  case class SetRate(permitsPerSecond: Double) extends Command

  val entityTypeKey: EntityTypeKey[Command] = EntityTypeKey("ClusterRate")

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] =
    ClusterSharding(system).init(Entity(entityTypeKey)(entityContext => apply(entityContext.entityId)))

  private[rate] def apply(name: String): Behavior[Command] = Behaviors.setup { context =>
    new ClusterRate(name, context).receive
  }
}

import cookbook.rate.ClusterRate._
class ClusterRate(name: String, context: ActorContext[Command]) {
  private val config = context.system.settings.config
  private val permitsPerSecond = config.getDouble("rate.permitsPerSecond")
  private val rate = RateLimiter.create(permitsPerSecond)
  println(s"The permitsPerSecond is $permitsPerSecond")

  def receive: Behavior[Command] = Behaviors.receiveMessage {
    case Acquire(n, replyTo) =>
      Behaviors.same
      val result = rate.tryAcquire(n)
      replyTo ! Acquired(result)
      Behaviors.same
    case SetRate(permitsPerSecond) =>
      rate.setRate(permitsPerSecond)
      Behaviors.same
  }
}
// #ClusterRate
