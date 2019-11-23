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

package cookbook.actor.lifecycle

import java.util.concurrent.TimeUnit

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.WordSpecLike

// #LifecycleTest
class LifecycleTest extends ScalaTestWithActorTestKit with WordSpecLike {
  "Lifecycle" should {
    "lifecycle" in {
      val ref = spawn(
        Behaviors.supervise(Lifecycle()).onFailure(SupervisorStrategy.restart))
      ref ! "hello"
      TimeUnit.SECONDS.sleep(1)
      ref ! "restart"
      TimeUnit.SECONDS.sleep(1)
    }
  }
}
// #LifecycleTest
