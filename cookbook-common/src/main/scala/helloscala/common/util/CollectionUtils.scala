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

import java.util.Objects

object CollectionUtils {
  def isEmpty(coll: Iterable[_]): Boolean = Objects.isNull(coll) || coll.isEmpty
  def isEmpty(coll: java.util.Collection[_]): Boolean =
    Objects.isNull(coll) || coll.isEmpty
  def nonEmpty(coll: Iterable[_]): Boolean = !isEmpty(coll)
  def nonEmpty(coll: java.util.Collection[_]): Boolean = !isEmpty(coll)
  def isSingle(coll: Iterable[_]): Boolean =
    Objects.nonNull(coll) && coll.size == 1
  def isSingle(coll: java.util.Collection[_]): Boolean =
    Objects.nonNull(coll) && coll.size == 1
}
