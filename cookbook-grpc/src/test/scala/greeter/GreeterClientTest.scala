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

package greeter

import java.util.concurrent.TimeUnit
import akka.NotUsed
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.adapter._
import akka.grpc.GrpcClientSettings
import akka.http.scaladsl.Http
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Sink, Source }
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._

class GreeterClientTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  private implicit val classicSystem = system.toClassic
  private implicit val ec = system.executionContext
  private var greeterClient: GreeterServiceClient = _

  // #GreeterService
  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val handler = GreeterServiceHandler(new GreeterServiceImpl())
    Http().bindAndHandleAsync(handler, "localhost", 8001)
    greeterClient = GreeterServiceClient(GrpcClientSettings.fromConfig(GreeterService.name))
  }
  // #GreeterService

  final case class MetricRequest()
  final case class MetricItem()
  // #sendMetrics
  def sendMetrics(in: MetricRequest): Source[MetricItem, NotUsed] = {
    val (queue, source) =
      Source.queue[MetricItem](16, OverflowStrategy.backpressure).preMaterialize()
    Source.tick(1.seconds, 1.seconds, MetricItem()).runForeach(metric => queue.offer(metric))
    source
  }
  // #sendMetrics

  "GreeterServiceClient" must {
    // #GreeterServiceClient
    "sayHello" in {
      greeterClient.sayHello(HelloRequest("Scala")).futureValue should ===(HelloReply("Hello, Scala."))
    }

    "itKeepsReplying" in {
      greeterClient.itKeepsReplying(HelloRequest("Scala")).take(5).runWith(Sink.seq).futureValue should ===(
        Seq(
          HelloReply("Hello, Scala; this is 1 times."),
          HelloReply("Hello, Scala; this is 2 times."),
          HelloReply("Hello, Scala; this is 3 times."),
          HelloReply("Hello, Scala; this is 4 times."),
          HelloReply("Hello, Scala; this is 5 times.")))
    }

    "itKeepsTalking" in {
      val (queue, in) =
        Source.queue[HelloRequest](16, OverflowStrategy.backpressure).preMaterialize()
      val f = greeterClient.itKeepsTalking(in)
      Seq("Scala", "Java", "Groovy", "Kotlin").foreach(program => queue.offer(HelloRequest(program)))
      TimeUnit.SECONDS.sleep(1)
      queue.complete()
      f.futureValue should ===(HelloReply("Hello, Scala, Java, Groovy, Kotlin."))
    }

    "streamHellos" in {
      val (queue, in) =
        Source.queue[HelloRequest](16, OverflowStrategy.backpressure).preMaterialize()
      val f = greeterClient.streamHellos(in).runWith(Sink.seq)
      Seq("Scala", "Java", "Groovy", "Kotlin").foreach(item => queue.offer(HelloRequest(item)))
      TimeUnit.SECONDS.sleep(1)
      queue.complete()
      f.futureValue should ===(
        Seq(
          HelloReply("Hello, Scala."),
          HelloReply("Hello, Java."),
          HelloReply("Hello, Groovy."),
          HelloReply("Hello, Kotlin.")))
    }
    // #GreeterServiceClient
  }
}
