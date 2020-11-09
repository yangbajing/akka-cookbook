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

import akka.actor.typed.{ ActorSystem, SpawnProtocol }
import akka.http.scaladsl.Http
import com.typesafe.scalalogging.StrictLogging

import scala.util.{ Failure, Success }

object GreeterApplication extends StrictLogging {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(SpawnProtocol(), "grpc")

    val handler = GreeterServiceHandler(new GreeterServiceImpl())
    val bindingF = Http().newServerAt("localhost", 8000).bind(handler)
    bindingF.onComplete {
      case Success(binding) =>
        logger.info(s"Greeter gRPC server started, bind to $binding.")
      case Failure(e) =>
        logger.error(s"Greeter gRPC server start failed：${e.getMessage}, exit.")
        system.terminate()
    }(system.executionContext)
  }
}
