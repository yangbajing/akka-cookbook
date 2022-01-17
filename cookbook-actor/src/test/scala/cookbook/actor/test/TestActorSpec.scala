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

package cookbook.actor.test

import akka.actor.testkit.typed.scaladsl.{ ScalaTestWithActorTestKit, TestProbe }
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import org.scalatest.wordspec.AnyWordSpecLike
import akka.actor.typed.scaladsl.AskPattern._
import org.scalatest.time.{ Milliseconds, Seconds, Span }

import scala.concurrent.duration._
import scala.util.Success

object TestActor {
  // #TestActor_messages
  sealed trait Command
  final case class Hello(message: String, replyTo: ActorRef[Reply]) extends Command

  sealed trait Reply
  final case class Answer(message: String) extends Reply
  // #TestActor_messages

  // #TestActor_apply
  def apply(): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.receiveMessage[Command] {
      case Hello(message, replyTo) =>
        replyTo ! Answer(s"Hi, you say is $message.")
        Behaviors.same
    }
  }
  // #TestActor_apply
}

class TestActorSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import TestActor._
  override implicit val patience =
    PatienceConfig(Span(5, Seconds), Span(10, Milliseconds))

  "Req-Resp" must {
    // #Req-Resp-probe
    "probe" in {
      val actor = spawn(TestActor(), "req-resp-probe")
      val probe: TestProbe[Reply] = createTestProbe[Reply]()

      actor ! Hello("Akka", probe.ref)
      probe.expectMessage(Answer("Hi, you say is Akka."))

      actor ! Hello("Scala", probe.ref)
      actor ! Hello("Java", probe.ref)

      probe.expectMessage(500.millis, Answer("Hi, you say is Scala."))
      probe.expectMessage(Answer("Hi, you say is Java."))
      probe.expectNoMessage(2.seconds)
    }
    // #Req-Resp-probe

    // #Req-Resp-ask
    "ask" in {
      val actor = spawn(TestActor())
      val answer = actor.ask[Reply](replyTo => Hello("Akka", replyTo)).mapTo[Answer].futureValue
      answer should ===(Answer("Hi, you say is Akka."))
    }
    // #Req-Resp-ask

    // #Req-Resp-async-assert
    "async-assert" in {
      val actor = spawn(TestActor())
      val answerF =
        actor.ask[Reply](replyTo => Hello("Akka", replyTo)).mapTo[Answer]
      eventually {
        answerF.value.get should be(Success(Answer("Hi, you say is Akka.")))
      }
    }
    // #Req-Resp-async-assert

    // #Req-Resp-eventually
    "eventually-success" in {
      val xs = 1 to 9
      val it = xs.iterator
      eventually { it.next should be(3) }
    }

    "eventually-failure" in {
      val xs = 1 to 9
      val it = xs.iterator
      eventually { it.next should be(10) }
    }
    // #Req-Resp-eventually
  }
}
