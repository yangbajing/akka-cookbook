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

package cookbook.streams.file

import java.nio.file.Files

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.stream.scaladsl.{ FileIO, Framing, Sink, Source }
import akka.util.ByteString
import org.scalatest.WordSpecLike

class FileIOTest extends ScalaTestWithActorTestKit with WordSpecLike {
  // #file-io-top
  private val LINE_SEPARATOR = ByteString("\n")
  private val file = Files.createTempFile("cookbook", "txt")
  private val TAKE_SIZE = 100
  // #file-io-top
  "FileIO" should {
    "toPath" in {
      // #to-path
      val f = Source
        .fromIterator(() => Iterator.from(0))
        .map(n => ByteString(n.toString))
        .take(TAKE_SIZE)
        .intersperse(ByteString.empty, LINE_SEPARATOR, LINE_SEPARATOR)
        .runWith(FileIO.toPath(file))
      val ioResult = f.futureValue
      ioResult.count should be > 0L
      // #to-path
    }

    "fromPath" in {
      // #from-path
      val f = FileIO
        .fromPath(file)
        .via(Framing.delimiter(LINE_SEPARATOR, 8192))
        .filterNot(_.isEmpty)
        .map(bytes => bytes.utf8String.toInt)
        .runWith(Sink.seq)
      val ints = f.futureValue
      ints should be(0 until 100)
      // #from-path
    }
  }
}
