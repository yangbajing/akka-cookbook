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

package cookbook.streams.startup

// #Startup
import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.Materializer
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

object Startup {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem(Behaviors.ignore, "streams")
    implicit val mat = Materializer(system)

    val source = Source.fromIterator(() => Iterator.from(0))
    val flow = Flow[Int].map(n => n.toString)
    val flowTake = Flow[String].take(10)
    val sink = Sink.foreach[String](str => println(str))
    val graph = source.via(flow).via(flowTake).toMat(sink)(Keep.right)

    val future: Future[Done] = graph.run()
    val result = Await.result(future, 10.seconds)
    println(s"Final result is $result")
  }
}
// #Startup
