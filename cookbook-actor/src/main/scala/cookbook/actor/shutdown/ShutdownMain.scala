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

package cookbook.actor.shutdown

import akka.Done
import akka.actor.{ ActorSystem, CoordinatedShutdown }

import scala.concurrent.Future

object ShutdownMain {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem()

    // #CoordinatedShutdown
    CoordinatedShutdown(system)
      .addCancellableTask(CoordinatedShutdown.PhaseServiceRequestsDone, "CloseJdbcDataSource") { () =>
        Future {
          println("Close JDBC DataSource.")
          Done
        }(system.dispatcher)
      }

    CoordinatedShutdown(system).addJvmShutdownHook {
      println("JVM shutdown hook.")
    }
    // #CoordinatedShutdown
    system.terminate()
  }
}
