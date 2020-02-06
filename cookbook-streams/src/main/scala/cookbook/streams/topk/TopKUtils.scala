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

import akka.util.ByteString

import scala.util.Try

object TopKUtils {
  // #toMovie
  def toMovie(bs: ByteString): Either[Throwable, Movie] =
    try {
      val arr = bs.utf8String.split(',')
      Right(Movie(arr(0), arr(1).toDouble))
    } catch {
      case e: Throwable => Left(e)
    }
  // #toMovie
}
