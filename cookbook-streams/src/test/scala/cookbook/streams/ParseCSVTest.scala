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
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.ByteString
import org.scalatest.FunSuiteLike

class ParseCSVTest extends ScalaTestWithActorTestKit with FunSuiteLike {
  test("file") {
    val text =
      """
        |eins,zwei,drei
        |"Yang Jing's computer, it is very power.",Chongqing,China
        |""".stripMargin
    val csvs = Source
      .single(ByteString(text))
      .via(CsvParsing.lineScanner())
      .map(_.map(_.utf8String))
      .runWith(Sink.seq)
      .futureValue
    csvs.foreach(println)
    println(csvs.size)
  }
}
