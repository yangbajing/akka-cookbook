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

package cookbook.streams.topk

import java.nio.file.Paths

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.scaladsl.FileIO

import scala.concurrent.Await
import scala.concurrent.duration._

object TopKForFile {
  def main(args: Array[String]): Unit = {
    // #TopKForFile
    implicit val system = ActorSystem(Behaviors.ignore, "topK")
    val res = Paths.get(Thread.currentThread().getContextClassLoader.getResource("movies.csv").toURI)

    val topKF = FileIO
      .fromPath(Paths.get(res.toUri))
      .via(CsvParsing.lineScanner())
      .drop(1) // Drop CSV Header
      .mapConcat {
        case name :: AsDouble(rating) :: _ => Movie(name.utf8String, rating) :: Nil
        case _                             => Nil
      }
      .runWith(new TopKSink(10))

    val topN = Await.result(topKF, 5.minutes)

    topN.foreach(println)
    println(topN.size)

    system.terminate()
    // #TopKForFile
  }
}
