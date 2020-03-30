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

package cookbook.action.oauth2.server.service.behavior

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityContext, EntityTypeKey }
import akka.persistence.typed.PersistenceId
import fusion.json.CborSerializable

object ClientEntity {
  sealed trait Command extends CborSerializable
  sealed trait Event extends CborSerializable
  sealed trait Response extends CborSerializable

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey("ClientEntity")

  def init(system: ActorSystem[_]): ActorRef[ShardingEnvelope[Command]] =
    ClusterSharding(system).init(Entity(TypeKey)(ec => apply(ec)))

  private def apply(ec: EntityContext[Command]): Behavior[Command] =
    Behaviors.setup(context =>
      new ClientEntity(ec.entityId, PersistenceId.of(ec.entityTypeKey.name, ec.entityId), context).receive())
}

import cookbook.action.oauth2.server.service.behavior.ClientEntity._
class ClientEntity private (clientId: String, id: PersistenceId, context: ActorContext[Command]) {
  def receive(): Behavior[Command] = Behaviors.receiveMessage { msg =>
    Behaviors.same
  }
}
