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

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.stream.alpakka.csv.scaladsl.CsvParsing

import scala.concurrent.Await
import scala.concurrent.duration._

object TopKForAkkaHTTP {
  def main(args: Array[String]): Unit = {
    // #TopKForAkkaHTTP
    implicit val system = ActorSystem(Behaviors.ignore, "topK")
    implicit val ec = system.executionContext
    val TOP_K = 10
    val URL =
      "https://gitee.com/yangbajing/akka-cookbook/raw/master/cookbook-streams/src/main/resources/movies.csv"

    val topKF = Http(system).singleRequest(HttpRequest(uri = URL)).flatMap { response =>
      response.entity.dataBytes
        .via(CsvParsing.lineScanner())
        .drop(1) // Drop CSV Header
        .mapConcat {
          case name :: AsDouble(rating) :: _ => Movie(name.utf8String, rating) :: Nil
          case _                             => Nil
        }
        .runWith(new TopKSink(TOP_K))
    }

    val topN = Await.result(topKF, 5.minutes)

    topN.foreach(println)
    println(topN.size)

    system.terminate()
    // #TopKForAkkaHTTP
  }
}
