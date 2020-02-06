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
      var buf = List[Movie]()
      var bufSize = 0

      def insertMovie(list: List[Movie], movie: Movie): List[Movie] = {
        list match {
          case Nil => movie :: Nil
          case list =>
            var buf = List[Movie]()
            var use = false
            for (item <- list.reverse) {
              if (!use && item.rating < movie.rating) {
                buf ::= movie
                use = true
              }
              buf ::= item
            }
            if (!use) {
              buf ::= movie
            }
            buf
        }
      }
      override def preStart(): Unit = pull(in)

      override def onPush(): Unit = {
        val movie = grab(in)
        buf = if (bufSize < TOP_K) {
          bufSize += 1
          insertMovie(buf, movie)
        } else {
          if (buf.head.rating < movie.rating) insertMovie(buf.slice(1, TOP_K), movie) else buf
        }
        pull(in)
      }

      override def onUpstreamFinish(): Unit = {
        p.trySuccess(buf)
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
