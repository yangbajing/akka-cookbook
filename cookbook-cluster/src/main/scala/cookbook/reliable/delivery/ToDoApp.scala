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
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorSystem, Behavior }
import akka.cluster.sharding.typed.delivery.{ ShardingConsumerController, ShardingProducerController }
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity, EntityTypeKey }
import akka.cluster.typed.Cluster

import scala.concurrent.Future

// #ToDoApp
object ToDoApp {
  private val entityTypeKey = EntityTypeKey[ConsumerController.SequencedMessage[TodoList.Command]]("toto")

  private def behavior(db: DB): Behavior[TodoList.Command] = Behaviors.setup { context =>
    val todoEntity = Entity(entityTypeKey)(entityContext =>
      ShardingConsumerController(start => TodoList(entityContext.entityId, db, start)))
    val todoRegion = ClusterSharding(context.system).init(todoEntity)
    val selfAddress = Cluster(context.system).selfMember.address
    val producerId = s"todo-producer-${selfAddress.host}:${selfAddress.port}"
    val producerController =
      context.spawn(ShardingProducerController(producerId, todoRegion, None), "producerController")

    context.spawn(TodoService(producerController), "producer")

    Behaviors.ignore
  }

  def main(args: Array[String]): Unit = {
    val db = new DB {
      override def save(id: String, value: TodoList.State): Future[Done] = ???
      override def load(id: String): Future[TodoList.State] = ???
    }
    ActorSystem(behavior(db), "todo-system")
  }
}
// #ToDoApp
