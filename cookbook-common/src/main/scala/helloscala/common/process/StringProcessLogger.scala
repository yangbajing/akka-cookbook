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

package helloscala.common.process

import scala.sys.process.ProcessLogger

class StringProcessLogger extends ProcessLogger {
  private var _outputs = Vector.empty[String]
  private var _errputs = Vector.empty[String]

  def outputs: Vector[String] = _outputs
  def errputs: Vector[String] = _errputs

  def outOrErrString: String =
    if (outputs.isEmpty) errputs.mkString("\n") else outputs.mkString("\n")
  def outStringAll: String =
    outputs.mkString("\n") + "\n\n" + errputs.mkString("\n")

  override def out(s: => String): Unit = {
    _outputs :+= s
  }

  override def err(s: => String): Unit = {
    _errputs :+= s
  }

  override def buffer[T](f: => T): T = f
}
