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

package helloscala.common.util

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import org.reactivestreams.Publisher

import scala.collection.immutable
import scala.concurrent.Future

object StreamUtils {
  def publishToHead[T](publisher: Publisher[T])(implicit mat: Materializer): Future[T] =
    Source.fromPublisher(publisher).runWith(Sink.head)

  def publishToHeadOption[T](publisher: Publisher[T])(implicit mat: Materializer): Future[Option[T]] =
    Source.fromPublisher(publisher).runWith(Sink.headOption)

  def publishToSeq[T](publisher: Publisher[T])(implicit mat: Materializer): Future[immutable.Seq[T]] =
    Source.fromPublisher(publisher).runWith(Sink.seq)

  def publishToIgnore[T](publisher: Publisher[T])(implicit mat: Materializer): Future[Done] =
    Source.fromPublisher(publisher).runWith(Sink.ignore)

  def sourceToPublish[T](source: Source[T, _])(implicit mat: Materializer): Publisher[T] =
    source.runWith(Sink.asPublisher(false))

  def sourceToPublishMultiple[T](source: Source[T, _])(implicit mat: Materializer): Publisher[T] =
    source.runWith(Sink.asPublisher(true))
}
