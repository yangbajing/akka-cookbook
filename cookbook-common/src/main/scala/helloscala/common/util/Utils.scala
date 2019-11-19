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

import java.lang.management.ManagementFactory
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Objects
import java.util.Properties
import java.util.UUID

import com.fasterxml.uuid.EthernetAddress
import com.fasterxml.uuid.Generators
import com.typesafe.scalalogging.StrictLogging
import helloscala.common.exception.HSException
import helloscala.common.exception.HSInternalErrorException

import scala.annotation.tailrec
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.util.control.NonFatal
import scala.util.matching.Regex

object Utils extends StrictLogging {
  val REGEX_DIGIT: Regex = """[\d,]+""".r
  val random: SecureRandom = new SecureRandom()
  private lazy val _timeBasedUuid = Generators.timeBasedGenerator(EthernetAddress.fromInterface())
  private lazy val _randomBasedUuid = Generators.randomBasedGenerator(new SecureRandom())

  @tailrec
  final def getValueFromFunctions[T](functions: Iterable[() => T], value: T, valueStopFunc: T => Boolean): T = {
    if (valueStopFunc(value)) value
    else if (functions.isEmpty) value
    else getValueFromFunctions(functions.tail, functions.head(), valueStopFunc)
  }

  def timeBasedUuid(): UUID = _timeBasedUuid.generate()

  def randomBasedUuid(): UUID = _randomBasedUuid.generate()

  def requireNonNull[T](v: T): T = requireNonNull(v, "requirement not null.")

  def requireNonNull[T](v: T, msg: String): T =
    try {
      Objects.requireNonNull(v)
    } catch {
      case _: Exception => throw HSInternalErrorException(msg)
    }

  def requireNonNull[T](v: T, e: => HSException): T =
    try {
      Objects.requireNonNull(v)
    } catch {
      case _: Exception => throw e
    }

  def require(predicate: Boolean): Unit = require(predicate, "requirement failed.")

  def require(predicate: Boolean, msg: String): Unit = {
    if (!predicate)
      throw HSInternalErrorException(msg)
  }

  def try2option[T](value: Try[T], log: Throwable => Unit): Option[T] = value match {
    case Success(v) => Some(v)
    case Failure(e) =>
      log(e)
      None
  }

  def using[T <: AutoCloseable, R](res: T)(func: T => R): R = {
    assert(res != null, "Resource res must not null")
    try {
      func(res)
    } finally {
      res.close()
    }
  }

  def quietly(f: => Unit): Unit =
    try {
      f
    } catch {
      case NonFatal(e) => // do nothing
        logger.warn(s"Quietly exception: ${e.toString}")
    }

  def quietly(f: => Unit, message: => String): Unit =
    try {
      f
    } catch {
      case NonFatal(_) => // do nothing
        logger.error(s"Quietly message: $message")
    }

  def quietlyRecover[T](f: => T)(recover: Throwable => T): T =
    try {
      f
    } catch {
      case e: Throwable => recover(e)
    }

  def timing[T](func: => T): (T, Duration) = {
    val begin = Instant.now()
    val result = func
    val end = Instant.now()
    val cost = java.time.Duration.between(begin, end)
    result -> cost
  }

  def swap[X, Y](x: X, y: Y): (Y, X) = (y, x)

  /**
   * 获取当前进程 pid
   */
  @inline def getPid: Long =
    try {
      java.lang.Long.parseLong(ManagementFactory.getRuntimeMXBean.getName.split("@")(0))
    } catch {
      case NonFatal(e) =>
        logger.error("getPid failure", e)
        -1
    }

  def either[T <: Throwable, R](func: => R): Either[T, R] =
    try {
      val result = func
      Right(result)
    } catch {
      case NonFatal(e) =>
        Left(e.asInstanceOf[T])
    }

  /**
   * 将字符串解析为数字
   *
   * @param s 字符串
   * @return
   */
  def parseInt(s: CharSequence): Option[Int] = AsInt.unapply(s)

  def parseInt(s: CharSequence, deft: => Int): Int =
    parseInt(s).getOrElse(deft)

  def parseInt(a: Any, deft: => Int): Int =
    parseInt(a.toString, deft)

  def parseIntAll(s: CharSequence): List[Int] = {
    val iter = REGEX_DIGIT.findAllIn(s)
    var list = List.empty[Int]
    while (iter.hasNext) {
      list = iter.next().toInt :: list
    }
    list
  }

  def parseLong(s: Any, deft: => Long): Long = parseLong(s).getOrElse(deft)

  def parseLong(s: Any): Option[Long] = AsLong.unapply(s)

  def isNoneBlank(content: String): Boolean = !isBlank(content)

  def isBlank(content: String): Boolean =
    content == null || content.isEmpty || content.forall(Character.isWhitespace)

  def byteBufferToArray(buf: ByteBuffer): Array[Byte] = {
    val dst = new Array[Byte](buf.remaining())
    buf.get(dst)
    dst
  }

  def randomBytes(size: Int): Array[Byte] = {
    val buf = new Array[Byte](size)
    random.nextBytes(buf)
    buf
  }

  def parseSeq(str: String, splitChar: Char = ','): Vector[String] =
    str.split(splitChar).filter(s => StringUtils.isNoneBlank(s)).toVector

  def mapToJMap[K, V](map: Map[K, V]): java.util.Map[K, V] = {
    val m = new java.util.HashMap[K, V]()
    map.foreach { case (k, v) => m.put(k, v) }
    m
  }

  def boxed(v: Any): Object = v match {
    case o: AnyRef  => o
    case i: Int     => Int.box(i)
    case l: Long    => Long.box(l)
    case d: Double  => Double.box(d)
    case s: Short   => Short.box(s)
    case f: Float   => Float.box(f)
    case c: Char    => Float.box(c)
    case b: Boolean => Boolean.box(b)
    case b: Byte    => Byte.box(b)
    case o          => o.asInstanceOf[Object]
  }

  def sqlBoxed(v: Any): Object = v match {
    case ldt: LocalDateTime => TimeUtils.toSqlTimestamp(ldt)
    case ld: LocalDate      => TimeUtils.toSqlDate(ld)
    case o                  => o.asInstanceOf[Object]
  }

  def boxedSQL(v: Any): Object =
    try {
      boxed(v)
    } catch {
      case _: Throwable =>
        sqlBoxed(v)
    }

  def isEmail(account: String): Boolean =
    // TODO
    account.contains('@')

  @inline
  def option(s: String): Option[String] =
    Some(s).filter(str => StringUtils.isNoneBlank(str))

  @inline
  def option[V](v: V): Option[V] = Option(v)

  @inline
  def some[T](v: T): Option[T] = Option(v)

  def propertiesToMap(props: Properties): Map[String, String] = {
    import scala.jdk.CollectionConverters._
    props.stringPropertyNames().asScala.map(name => name -> props.getProperty(name)).toMap
  }

  def propertiesToMapObject(props: Properties): Map[String, Object] = {
    import scala.jdk.CollectionConverters._
    props.stringPropertyNames().asScala.map(name => name -> props.get(name)).toMap
  }

  def closeQuiet(io: AutoCloseable): Unit = {
    if (io ne null) try {
      io.close()
    } catch {
      case _: Throwable => // do nothing
    }
  }
}
