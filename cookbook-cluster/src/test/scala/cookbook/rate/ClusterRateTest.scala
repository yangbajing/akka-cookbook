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

package cookbook.rate

import java.util.concurrent.TimeUnit

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.AskPattern._
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyAnyWordSpecLike

/**
 * @author Yang Jing <a href="mailto:yang.xunjing@qq.com">yangbajing</a>
 * @date 2020-11-11 21:43:00
 */
// #ClusterRateTest
class ClusterRateTest
    extends ScalaTestWithActorTestKit("""{
    |akka.actor.provider = cluster
    |akka.remote.artery.canonical.hostname = 127.0.0.1
    |akka.remote.artery.canonical.port = 25520
    |akka.cluster.seed-nodes = ["akka://ClusterRateTest@127.0.0.1:25520"]
    |rate.permitsPerSecond = 2}""".stripMargin)
    with AnyAnyWordSpecLike
    with BeforeAndAfterAll {
  override protected def beforeAll(): Unit = {
    ClusterRate.init(system)
  }

  "ClusterRate" should {
    "acquire" in {
      val rateLimiterRef = ClusterSharding(system).entityRefFor(ClusterRate.entityTypeKey, "rateLimiter")

      val acquireResult1 = rateLimiterRef.ask(replyTo => ClusterRate.Acquire(1, replyTo)).futureValue
      acquireResult1.result shouldBe true

      val acquireResult2 = rateLimiterRef.ask(replyTo => ClusterRate.Acquire(1, replyTo)).futureValue
      acquireResult2.result shouldBe false

      TimeUnit.MILLISECONDS.sleep(600)

      val acquireResult3 = rateLimiterRef.ask(replyTo => ClusterRate.Acquire(1, replyTo)).futureValue
      acquireResult3.result shouldBe true
    }
  }
}
// #ClusterRateTest
