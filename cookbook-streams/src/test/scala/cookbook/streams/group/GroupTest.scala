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

package cookbook.streams.group

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.stream.scaladsl.{ Sink, Source }
import org.scalatest.WordSpecLike

import scala.concurrent.Await
import scala.concurrent.duration._

class GroupTest extends ScalaTestWithActorTestKit with WordSpecLike {
  "Group" should {
    // #grouped
    "grouped" in {
      val list = Source.fromIterator(() => Iterator.from(0)).grouped(100).take(2).runWith(Sink.seq).futureValue
      list should be(Vector(0 until 100, 100 until 200))
    }
    // #grouped
    // #groupedWithin
    "groupedWithin" in {
      val f = Source
        .fromIterator(() => Iterator.from(0))
        .throttle(5, 500.millis)
        .groupedWithin(100, 1.seconds)
        .take(2)
        .runWith(Sink.seq)
      val list = Await.result(f, 3.seconds)
      list should not be Vector(0 until 100, 100 until 200)
    }
    // #groupedWithin
  }
}
