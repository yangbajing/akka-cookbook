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

import java.time.format.DateTimeFormatter
import java.time._
import java.util.Date
import java.sql.{ Date => SQLDate }
import java.sql.{ Time => SQLTime }
import java.sql.{ Timestamp => SQLTimestamp }

import com.typesafe.scalalogging.StrictLogging

import scala.util.Try
import scala.util.control.NonFatal

object TimeUtils extends StrictLogging {
  val DATE_TIME_EPOCH: LocalDateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)
  val ZONE_CHINA_OFFSET: ZoneOffset = ZoneOffset.ofHours(8)

  val formatterDateTime: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  val formatterDateTimeMillisCompact: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
  val formatterMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
  val formatterDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  val formatterDateHours: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH")

  val formatterDateMinutes: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
  val formatterMinutes: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
  val formatterTime: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

  val formatterYear: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy")
  val formatterOnlyMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("MM")

  val DateKeys = List("年", "月", "-", "/", "日")

  def nowTimestamp(): SQLTimestamp = SQLTimestamp.from(Instant.now())

  /**
   * 函数执行时间
   *
   * @param func 待执行函数
   * @tparam R func函数返回类型
   * @return (func函数返回值，函数执行时间纳秒数)
   */
  def executeNanoTime[R](func: => R): (R, Long) = {
    val begin = System.nanoTime()
    val result = func
    val end = System.nanoTime()
    (result, end - begin)
  }

  /**
   * 函数执行时间
   *
   * @param func 待执行函数
   * @tparam R func函数返回类型
   * @return (func函数返回值，函数执行时间毫秒数)
   */
  def executeMillis[R](func: => R): (R, Long) = {
    val begin = System.currentTimeMillis()
    val result = func
    val end = System.currentTimeMillis()
    (result, end - begin)
  }

  def dumpExecuteTime[R](func: => R): R = {
    val begin = System.currentTimeMillis()
    val result = func
    val duration = Duration.ofMillis(System.currentTimeMillis() - begin)
    logger.info(s"函数执行时间：$duration")
    result
  }

  /**
   * 解析字符串为 LocalDate
   *
   * @param date 字符串形式日期
   * @return 成功返回 LocalDate
   */
  def toLocalDate(date: String): LocalDate =
    Try(LocalDate.parse(date, formatterDate)).getOrElse {
      val (year, month, day) = date.split("""[-/:]""") match {
        case Array(y, m, d) =>
          (y.toInt, m.toInt, d.toInt)
        case Array(y, m) =>
          (y.toInt, m.toInt, 1)
        case Array(y) =>
          (y.toInt, 1, 1)
        case _ =>
          throw new IllegalArgumentException(s"$date is invalid iso date format")
      }

      if (year < 0 || year > 9999)
        throw new IllegalArgumentException(s"$date is invalid iso date format ($year)")

      LocalDate.of(year, month, day)
    }

  /**
   * 解析字符串为 LocalTime
   *
   * @param time 字符串形式时间
   * @return 成功返回 LocalTime
   */
  def toLocalTime(time: String): LocalTime =
    Try(LocalTime.parse(time, formatterTime)).getOrElse {
      val (hour, minute, second, nano) =
        time.split("""[:-]""") match {
          case Array(h, m, s) =>
            s.split('.') match {
              case Array(sec, millis) =>
                (h.toInt, m.toInt, sec.toInt, millis.toInt * 1000 * 1000)
              case arr =>
                (h.toInt, m.toInt, arr(0).toInt, 0)
            }
          case Array(h, m) =>
            (h.toInt, m.toInt, 0, 0)
          case Array(h) =>
            (h.toInt, 0, 0, 0)
          case _ =>
            throw new IllegalArgumentException(s"$time is invalid iso time format")
        }

      LocalTime.of(hour, minute, second, nano)
    }

  /**
   * 解析字符串为 LocalDateTime
   *
   * @param date 字符串形式日期
   * @param time 字符串形式时间
   * @return 成功返回 LocalDateTime
   */
  def toLocalDateTime(date: String, time: String): LocalDateTime = {
    val d =
      if (StringUtils.isNoneBlank(date)) toLocalDate(date) else LocalDate.now()
    val t =
      if (StringUtils.isNoneBlank(time)) toLocalTime(time) else LocalTime.now()
    LocalDateTime.of(d, t)
  }

  /**
   * 解析字符串为 LocalDateTime
   *
   * @param ldt 字符串形式日期时间
   * @return 成功返回 LocalDateTime
   */
  def toLocalDateTime(ldt: String): LocalDateTime =
    Try(LocalDateTime.parse(ldt, formatterDateTime)).getOrElse {
      ldt.split("""[ Tt]+""") match {
        case Array(date, time) =>
          toLocalDateTime(date, time)
        case Array(dOrT) =>
          if (containsDateKeys(dOrT)) toLocalDateTime(dOrT, "")
          else toLocalDateTime("", dOrT)
        case _ =>
          throw new DateTimeException(s"$ldt 是无效的日期时间格式，推荐格式：yyyy-MM-dd HH:mm:ss")
      }
    }

  private def containsDateKeys(dOrT: String) =
    DateKeys.exists(v => dOrT.contains(v))

  def toLocalDateTime(instant: Instant): LocalDateTime =
    LocalDateTime.ofInstant(instant, ZONE_CHINA_OFFSET)

  def toLocalDateTime(epochMilli: Long): LocalDateTime =
    toLocalDateTime(Instant.ofEpochMilli(epochMilli))

  def toLocalDateTime(date: Date): LocalDateTime =
    if (date eq null) null
    else toLocalDateTime(date.toInstant)

  private val OFFSET_REGEX = "([:\\d-+]+)".r

  def zoneOf(str: String): ZoneId = str match {
    case OFFSET_REGEX(offset) => zoneOffsetOf(offset)
    case _ =>
      val begin = str.indexOf('[') + 1
      if (begin > 0) {
        require(begin > 0 && begin < str.length, s"$str 无有效的'['符号")
        val end = str.indexOf(']')
        require(end > begin && end < str.length, s"$str 无有效的']'符号")
        val id = str.substring(begin, end)
        ZoneId.of(id)
      } else {
        ZoneId.of(str)
      }
  }

  def zoneOffsetOf(str: String): ZoneOffset =
    if (str.indexOf('-') >= 0 || str.indexOf('+') >= 0) ZoneOffset.of(str)
    else if (str == "Z") ZoneOffset.UTC
    else ZoneOffset.of(str)

  private val TIME_REGEX = "([:\\d\\.]+)".r

  def toZonedDateTime(zdt: String): ZonedDateTime =
    try {
      zdt.split("""[ Tt]+""") match {
        case Array(date, timeAndZone) =>
          val time =
            TIME_REGEX.findFirstIn(timeAndZone).getOrElse(throw new DateTimeException(s"$timeAndZone 不能截取有效的时间部分"))
          val zone = Utils.option(timeAndZone.replaceFirst(time, "")).map(zoneOf).getOrElse(ZONE_CHINA_OFFSET)
          toZonedDateTime(date, time, zone)
        case Array(dOrT) =>
          if (containsDateKeys(dOrT)) toZonedDateTime(dOrT, "")
          else toZonedDateTime("", dOrT)
        case _ =>
          throw new DateTimeException(s"$zdt 是无效的日期时间格式，推荐格式：yyyy-MM-dd HH:mm:ss[+Z]")
      }
    } catch {
      case ex: Throwable =>
        try {
          ZonedDateTime.parse(zdt)
        } catch {
          case NonFatal(e) =>
            e.initCause(ex)
            logger.warn(s"toZonedDateTime error: $zdt", e)
            throw e
        }
    }

  def toZonedDateTime(date: String, time: String): ZonedDateTime =
    toZonedDateTime(date, time, ZONE_CHINA_OFFSET)

  def toZonedDateTime(date: String, time: String, zoneId: ZoneId): ZonedDateTime =
    toLocalDateTime(date, time).atZone(zoneId)

  def toZonedDateTime(epochMillis: Long): ZonedDateTime =
    toOffsetDateTime(epochMillis).toZonedDateTime

  def toOffsetDateTime(epochMillis: Long): OffsetDateTime =
    Instant.ofEpochMilli(epochMillis).atOffset(ZONE_CHINA_OFFSET)

  def toOffsetDateTime(zdt: String): OffsetDateTime =
    try {
      zdt.split("""[ Tt]+""") match {
        case Array(date, timezone) =>
          val (time, zone) = timezone.split("""[+-]""") match {
            case Array(timeStr, zoneStr) =>
              (timeStr, zoneOffsetOf((if (timezone.indexOf('-') < 0) '+' else '-') + zoneStr))
            case Array(timeStr) => (timeStr, ZONE_CHINA_OFFSET)
            case _ =>
              throw new DateTimeException(s"$zdt 无有效的时区信息，推荐格式：yyyy-MM-dd HH:mm:ss[+Z]")
          }
          toOffsetDateTime(date, time, zone)
        case Array(dOrT) =>
          if (containsDateKeys(dOrT)) toOffsetDateTime(dOrT, "")
          else toOffsetDateTime("", dOrT)
        case _ =>
          throw new DateTimeException(s"$zdt 是无效的日期时间格式，推荐格式：yyyy-MM-dd HH:mm:ss[+Z]")
      }
    } catch {
      case e: Exception =>
        logger.warn(s"toZonedDateTime error: $zdt")
        throw e
    }

  def toOffsetDateTime(date: String, time: String): OffsetDateTime =
    toOffsetDateTime(date, time, ZONE_CHINA_OFFSET)

  def toOffsetDateTime(date: String, time: String, zoneOffset: ZoneOffset): OffsetDateTime =
    toLocalDateTime(date, time).atOffset(zoneOffset)

  def toDate(ldt: LocalDateTime): Date =
    Date.from(ldt.toInstant(ZONE_CHINA_OFFSET))

  def toDate(zdt: ZonedDateTime): Date = Date.from(zdt.toInstant)

  def toEpochMilli(dt: LocalDateTime): Long =
    dt.toInstant(ZONE_CHINA_OFFSET).toEpochMilli

  def toEpochMilli(dt: ZonedDateTime): Long =
    dt.toInstant.toEpochMilli

  def toEpochMilli(dt: OffsetDateTime): Long =
    dt.toInstant.toEpochMilli

  @deprecated("使用toEpochMilli(dt: LocalDateTime)或toEpochMilli(odt: OffsetDateTime)", "1.0.0")
  def toEpochMilli(dt: String): Long =
    toLocalDateTime(dt).toInstant(ZONE_CHINA_OFFSET).toEpochMilli

  def toSqlTimestamp(dt: LocalDateTime): SQLTimestamp = SQLTimestamp.valueOf(dt)

  def toSqlTimestamp(zdt: ZonedDateTime): SQLTimestamp =
    SQLTimestamp.from(zdt.toInstant)

  def toSqlTimestamp(ist: Instant): SQLTimestamp = SQLTimestamp.from(ist)

  def toSqlDate(date: LocalDate) =
    new SQLDate(toEpochMilli(date.atStartOfDay()))

  def toSqlTime(time: LocalTime) =
    new SQLTime(toEpochMilli(time.atDate(LocalDate.now())))

  def toSqlTime(time: String): SQLTime = SQLTime.valueOf(time)

  /**
   * @return 一天的开始：
   */
  @inline def nowBegin(): LocalDateTime = LocalDate.now().atTime(0, 0, 0, 0)

  /**
   * @return 一天的结尾：
   */
  @inline def nowEnd(): LocalDateTime =
    LocalTime.of(23, 59, 59, 999999999).atDate(LocalDate.now())

  @inline def toDayInt(ldt: LocalDateTime): Int = toDayInt(ldt.toLocalDate)

  @inline def toDayInt(odt: OffsetDateTime): Int = toDayInt(odt.toLocalDate)

  @inline def toDayInt(zdt: ZonedDateTime): Int = toDayInt(zdt.toLocalDate)

  /**
   * 将 LocalDate(2017-11-21) 转换成 20171121 (Int类型)
   *
   * @param d 日期对象
   * @return
   */
  def toDayInt(d: LocalDate): Int =
    d.getYear * 10000 + d.getMonthValue * 100 + d.getDayOfMonth

  @inline def toTimeInt(ldt: LocalDateTime): Int = toTimeInt(ldt.toLocalTime)

  def toTimeInt(lt: LocalTime): Integer =
    lt.getHour * 10000 + lt.getMinute * 100 + lt.getSecond

  private[this] lazy val funcId = new java.util.concurrent.atomic.AtomicLong()

  def time[R](func: => R): R = {
    val fid = funcId.incrementAndGet()
    val start = Instant.now()
    logger.info(s"funcId: $fid start time: $start")
    try {
      func
    } finally {
      val end = Instant.now()
      val cost = Duration.between(start, end)
      logger.info(s"funcId: $fid end time: $end, cost: $cost")
    }
  }
}
