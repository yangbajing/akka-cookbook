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

package demo

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.Source

import scala.concurrent.Await
import scala.concurrent.duration._

object Demo extends App {
  implicit val system = ActorSystem(Behaviors.ignore, "sum")
  val begin = System.nanoTime()

  val f = Source
    .fromIterator(() => Iterator.from(0))
    .take(Int.MaxValue >> 8)
    .splitWhen(_ % 100000 == 0L)
    .async
    .fold(0L)(_ + _)
    .async
    .mergeSubstreams
    .runFold(0L)(_ + _)

  val result = Await.result(f, Duration.Inf)

  val cost = System.nanoTime() - begin
  println(s"Calculated result is $result, takes ${cost.nanos.toMillis}ms.")
  system.terminate()
}
