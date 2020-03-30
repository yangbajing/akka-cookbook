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

import akka.stream.stage.{ GraphStageLogic, GraphStageWithMaterializedValue, InHandler }
import akka.stream.{ AbruptStageTerminationException, Attributes, Inlet, SinkShape }

import scala.concurrent.{ Future, Promise }

// #TopKSink
class TopKSink(TOP_K: Int) extends GraphStageWithMaterializedValue[SinkShape[Movie], Future[List[Movie]]] {
  val in: Inlet[Movie] = Inlet("TopKSink.in")

  override def shape: SinkShape[Movie] = SinkShape(in)

  override def createLogicAndMaterializedValue(
      inheritedAttributes: Attributes): (GraphStageLogic, Future[List[Movie]]) = {
    val p: Promise[List[Movie]] = Promise()

    val logic = new GraphStageLogic(shape) with InHandler {
      private implicit val movieOrdering: Ordering[Movie] =
        (x: Movie, y: Movie) => if (x.rating < y.rating) 1 else if (x.rating == y.rating) 0 else -1
      private val queue = scala.collection.mutable.PriorityQueue[Movie]()

      override def preStart(): Unit = pull(in)

      override def onPush(): Unit = {
        val movie = grab(in)
        // #append-movie
        if (queue.size < TOP_K) {
          queue.addOne(movie)
        } else if (queue.head.rating < movie.rating) {
          queue.addOne(movie).dequeue()
        }
        // #append-movie
        pull(in)
      }

      override def onUpstreamFinish(): Unit = {
        p.trySuccess(queue.toList)
      }

      override def onUpstreamFailure(ex: Throwable): Unit = {
        p.tryFailure(ex)
        failStage(ex)
      }

      override def postStop(): Unit = {
        if (!p.isCompleted) p.failure(new AbruptStageTerminationException(this))
      }

      setHandler(in, this)
    }

    (logic, p.future)
  }
}
// #TopKSink
