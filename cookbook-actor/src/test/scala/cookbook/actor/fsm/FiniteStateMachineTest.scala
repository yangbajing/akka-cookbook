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

package cookbook.actor.fsm

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.FunSuiteLike

class FiniteStateMachineTest extends ScalaTestWithActorTestKit with FunSuiteLike {
  test("FiniteStateMachine") {
    // #FiniteStateMachine
    val ref = spawn(FiniteStateMachine(), "fsm")
    ref ! "message 1"
    ref ! "message 2"
    ref ! "running"
    ref ! "message 3"
    ref ! "message 4"
    ref ! "passive"
    ref ! "message 5"
    // #FiniteStateMachine
  }
}
