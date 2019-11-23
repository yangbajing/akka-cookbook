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

package cookbook.streams

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Keep, Sink, Source }
import org.scalatest.WordSpecLike

class SourceTest extends ScalaTestWithActorTestKit with WordSpecLike {
  "Cycle Source" must {
    "continuously generate the same sequence" in {
      //#cycle
      Source
        .cycle(() => List(1, 2, 3).iterator)
        .take(10)
        .runWith(Sink.seq)
        .futureValue should be(Seq(1, 2, 3, 1, 2, 3, 1, 2, 3, 1))
      //#cycle
    }
  }

  "Queue Source" must {
    "generate fixed sequence" in {
      // #queue
      val (queue, result) = Source
        .queue[Int](10, OverflowStrategy.dropNew)
        .toMat(Sink.seq)(Keep.both)
        .run()
      queue.offer(10)
      queue.offer(10)
      queue.offer(10)
      queue.complete()
      result.futureValue should be(Seq(10, 10, 10))
      // #queue
    }
  }
}
