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

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.stream.IOResult
import akka.stream.scaladsl.{ FileIO, Framing, Keep, Sink, Source }
import akka.util.ByteString
import org.scalatest.WordSpecLike

import scala.concurrent.Future

class MaterializeTest extends ScalaTestWithActorTestKit with WordSpecLike {
  "Materializer" must {
    "keep" in {
      // #keep
      val source: Source[ByteString, Future[IOResult]] = FileIO.fromPath(Paths.get("/tmp/file.txt"))
      val sink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(Paths.get("/tmp/file2.txt"))
      val graph: Source[ByteString, Future[IOResult]] = source // (1)
        .via(Framing.delimiter(ByteString("\n"), 8192))
        .filterNot(_.isEmpty)

      val readsF: Future[IOResult] = // (2)
        graph.toMat(sink)(Keep.left).run() // Same: graph.to(sink).run()
      val writesF: Future[IOResult] = // (3)
        graph.toMat(sink)(Keep.right).run() // Same: graph.runWith(sink)
      val (leftF, rightF) = graph.toMat(sink)(Keep.both).run() // (4)
      val notUsed: NotUsed = graph.toMat(sink)(Keep.none).run() // (5)
      // #keep
      readsF.futureValue.count should be > 0L
      writesF.futureValue.count should be > 0L
      leftF.futureValue.count should be > 0L
      rightF.futureValue.count should be > 0L
      notUsed should be(NotUsed)
    }
  }
}
