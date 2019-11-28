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

import java.sql
import java.sql.Timestamp
import java.time._
import java.util.Date

import scala.util.Try
import scala.util.matching.Regex

object AsString {
  def unapply(v: Any): Option[String] = v match {
    case null      => None
    case s: String => Some(s)
    case v: AnyRef => Some(v.toString)
    case _         => Some(v.toString)
  }
}

object AsChar {
  def unapply(v: Any): Option[Char] = v match {
    case null                               => None
    case c: Char                            => Some(c)
    case c: Character                       => Some(c)
    case s: CharSequence if s.length() == 1 => Try(s.charAt(0)).toOption
    case _                                  => None
  }
}

object AsByte {
  def unapply(v: Any): Option[Byte] = v match {
    case null              => None
    case b: Byte           => Some(b)
    case b: java.lang.Byte => Some(b)
    case s: CharSequence   => Try(s.toString.toByte).toOption
    case AsShort(s)        => Some(s.toByte)
    case _                 => None
  }
}

object AsBoolean {
  def str2bool(str: String): Option[Boolean] = str.toLowerCase match {
    case "true"  => Some(true)
    case "false" => Some(false)
    case _       => None
  }

  def unapply(v: Any): Option[Boolean] = v match {
    case null                 => None
    case b: Boolean           => Some(b)
    case b: java.lang.Boolean => Some(b)
    case AsString(str)        => str2bool(str)
    case AsInt(i)             => if (i == 1) Some(true) else Some(false)
    case _                    => None
  }
}

object AsShort {
  def unapply(v: Any): Option[Short] = v match {
    case null               => None
    case s: Short           => Some(s)
    case s: java.lang.Short => Some(s)
    case s: CharSequence    => Try(s.toString.toShort).toOption
    case AsInt(i)           => Some(i.toShort)
    case _                  => None
  }
}

object AsInt {
  def unapply(v: Any): Option[Int] = v match {
    case null            => None
    case i: Int          => Some(i)
    case i: Integer      => Some(i)
    case s: CharSequence => toInt(s)
    case AsLong(l)       => Some(l.toInt)
    case _               => None
  }

  def toInt(s: CharSequence): Option[Int] =
    AsLong.REGEX_DIGIT.findFirstIn(s).map(_.replaceAll(",", "").toInt).orElse(Try(s.toString.toInt).toOption)
}

object AsLong {
  val REGEX_DIGIT: Regex = """[\d,]+""".r

  def unapply(v: Any): Option[Long] = v match {
    case null              => None
    case l: Long           => Some(l)
    case l: java.lang.Long => Some(l)
    case bi: BigInt        => Some(bi.longValue)
    case s: CharSequence   => toLong(s)
    case _                 => None
  }

  def toLong(s: CharSequence): Option[Long] =
    REGEX_DIGIT.findFirstIn(s).map(_.replaceAll(",", "").toLong).orElse(Try(s.toString.toLong).toOption)
}

object AsFloat {
  def unapply(v: Any): Option[Float] = v match {
    case null               => None
    case f: Float           => Some(f)
    case f: java.lang.Float => Some(f)
    case AsDouble(d)        => Some(d.toFloat)
    case _                  => None
  }
}

/**
 * 小数点后14位
 */
object AsDouble {
  def unapply(v: Any): Option[Double] = v match {
    case null                => None
    case d: Double           => Some(d)
    case d: java.lang.Double => Some(d)
    case s: String           => Try(s.toDouble).toOption
    case _                   => None
  }
}

object AsDate {
  def unapply(v: AnyRef): Option[Date] = v match {
    case null           => None
    case d: Date        => Some(d)
    case AsInstant(ist) => Some(Date.from(ist))
    case _              => None
  }
}

object AsSQLDate {
  def unapply(v: AnyRef): Option[sql.Date] = v match {
    case d: sql.Date     => Some(d)
    case AsTimestamp(ts) => Some(new sql.Date(ts.getTime))
    case s: String       => Try(sql.Date.valueOf(s)).toOption
    case _               => None
  }
}

object AsSQLTime {
  def unapply(v: AnyRef): Option[sql.Time] = v match {
    case t: sql.Time     => Some(t)
    case AsTimestamp(ts) => Some(new sql.Time(ts.getTime))
    case s: String       => Try(sql.Time.valueOf(s)).toOption
    case _               => None
  }
}

object AsTimestamp {
  def unapply(v: AnyRef): Option[Timestamp] = v match {
    case null           => None
    case ts: Timestamp  => Some(ts)
    case AsInstant(ist) => Some(Timestamp.from(ist))
    case _              => None
  }
}

object AsInstant {
  def unapply(v: AnyRef): Option[Instant] = v match {
    case null         => None
    case ins: Instant => Some(ins)
    case s: String    => Try(Instant.parse(s)).toOption
    case _            => None
  }
}

object AsLocalDate {
  def unapply(v: AnyRef): Option[LocalDate] = v match {
    case null                 => None
    case ld: LocalDate        => Some(ld)
    case d: sql.Date          => Some(d.toLocalDate)
    case d: Date              => Some(d.toInstant.atZone(ZoneId.systemDefault()).toLocalDate)
    case AsLocalDateTime(ldt) => Some(ldt.toLocalDate)
    case _                    => None
  }
}

object AsLocalTime {
  def unapply(v: AnyRef): Option[LocalTime] = v match {
    case null                 => None
    case lt: LocalTime        => Some(lt)
    case t: sql.Time          => Some(t.toLocalTime)
    case AsLocalDateTime(ldt) => Some(ldt.toLocalTime)
    case _                    => None
  }
}

object AsLocalDateTime {
  def unapply(v: AnyRef): Option[LocalDateTime] = v match {
    case null                 => None
    case ldt: LocalDateTime   => Some(ldt)
    case AsZonedDateTime(zdt) => Some(zdt.toLocalDateTime)
    case s: String =>
      Try(TimeUtils.toLocalDateTime(s)).orElse(Try(LocalDateTime.parse(s))).toOption
    case _ => None
  }
}

object AsZonedDateTime {
  def unapply(v: Any): Option[ZonedDateTime] = v match {
    case null               => None
    case zdt: ZonedDateTime => Some(zdt)
    case s: String =>
      Try(ZonedDateTime.parse(s)).orElse(Try(TimeUtils.toZonedDateTime(s.toLong))).toOption
    case epochMillis: Long =>
      Try(TimeUtils.toZonedDateTime(epochMillis)).toOption
    case AsOffsetDateTime(odt) => Some(odt.toZonedDateTime)
    case _                     => None
  }
}

object AsOffsetDateTime {
  def unapply(v: Any): Option[OffsetDateTime] = v match {
    case null                => None
    case odt: OffsetDateTime => Some(odt)
    case s: String =>
      Try(TimeUtils.toOffsetDateTime(s)).orElse(Try(TimeUtils.toOffsetDateTime(s.toLong))).toOption
    case epochMillis: Long =>
      Try(TimeUtils.toOffsetDateTime(epochMillis)).toOption
    case _ => None
  }
}
