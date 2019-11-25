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

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

import org.scalatest.FunSuite
import org.scalatest.MustMatchers

class TimeUtilsTest extends FunSuite with MustMatchers {
  test("testToLocalTime") {
    TimeUtils.toLocalTime("22:11:32") mustBe LocalTime.of(22, 11, 32)
    TimeUtils.toLocalTime("8:7:32") mustBe LocalTime.of(8, 7, 32)
  }

  test("testToSqlDate") {
    TimeUtils.toSqlDate(LocalDate.of(2019, 2, 28)) mustBe java.sql.Date
      .valueOf("2019-2-28")
  }

  test("testToSqlTimestamp") {
    val sqlTimestamp =
      TimeUtils.toSqlTimestamp(LocalDateTime.of(2019, 4, 15, 11, 22, 33))
    sqlTimestamp mustBe java.sql.Timestamp.valueOf("2019-04-15 11:22:33")
  }

  test("testNowEnd") {
    val nowEnd = TimeUtils.nowEnd()
    nowEnd mustBe LocalDateTime.of(
      LocalDate.now(),
      LocalTime.of(23, 59, 59, 999999999))
  }

  test("testToLocalDateTime") {
    val instant = Instant.now()
    val ldt = LocalDateTime.ofInstant(
      instant,
      TimeUtils.ZONE_CHINA_OFFSET /*ZoneId.systemDefault()*/ )
    TimeUtils.toLocalDateTime(java.util.Date.from(instant)) mustBe ldt
    TimeUtils.toLocalDateTime(instant.toEpochMilli) mustBe ldt
    TimeUtils.toLocalDateTime("2019-4-9 12:9:9") mustBe LocalDateTime.of(2019, 4, 9,
      12, 9, 9)
    TimeUtils.toLocalDateTime(instant) mustBe ldt
    TimeUtils.toLocalDateTime("2019-5-11", "12:8:4") mustBe LocalDateTime.of(2019,
      5, 11, 12, 8, 4)
  }

  test("testNowTimestamp") {
    val sqlTimestamp = TimeUtils.nowTimestamp()
    sqlTimestamp must not be null
  }

  test("testZoneOffsetOf") {
    TimeUtils.zoneOffsetOf("+08").getTotalSeconds mustBe 28800
    TimeUtils.zoneOffsetOf("-08").getTotalSeconds mustBe -28800
    TimeUtils.zoneOffsetOf("+0830").getTotalSeconds mustBe 30600
  }

  test("testToOffsetDateTime") {
    TimeUtils.toOffsetDateTime("2019-4-9", "11:22:33") mustBe
    OffsetDateTime.of(2019, 4, 9, 11, 22, 33, 0, TimeUtils.ZONE_CHINA_OFFSET)

    val ts = System.currentTimeMillis()
    TimeUtils.toOffsetDateTime(ts) mustBe
    OffsetDateTime.ofInstant(Instant.ofEpochMilli(ts), TimeUtils.ZONE_CHINA_OFFSET)

    TimeUtils.toOffsetDateTime("2019-4-9", "11:22:33", ZoneOffset.UTC) mustBe
    OffsetDateTime.of(2019, 4, 9, 11, 22, 33, 0, ZoneOffset.UTC)

    TimeUtils.toOffsetDateTime("2019-4-9 11:22:33+1230") mustBe
    OffsetDateTime.of(2019, 4, 9, 11, 22, 33, 0, ZoneOffset.of("+1230"))
  }

  test("testToZonedDateTime") {
    val zdt =
      ZonedDateTime.of(2019, 4, 9, 11, 7, 22, 0, ZoneId.of("Asia/Shanghai"))
    TimeUtils.toZonedDateTime("2019-04-09T11:07:22+08:00[Asia/Shanghai]") mustBe zdt
    TimeUtils.toZonedDateTime("2019-4-9T11:07:22+08[Asia/Shanghai]") mustBe zdt
    TimeUtils.toZonedDateTime("2019-4-9 11:7:22+08[Asia/Shanghai]") mustBe zdt
    TimeUtils.toZonedDateTime("2019-4-9 11:7:22.333+08[Asia/Shanghai]") mustBe
    zdt.plusNanos(TimeUnit.MILLISECONDS.toNanos(333))
  }

  test("testToDate") {
    TimeUtils.toDate(LocalDateTime.now()) must not be null
    TimeUtils.toDate(ZonedDateTime.now()) must not be null
  }

  test("testToEpochMilli") {
    val ldt = LocalDateTime.of(2019, 4, 9, 16, 18, 4)
    TimeUtils.toEpochMilli(ldt) mustBe ldt
      .toInstant(TimeUtils.ZONE_CHINA_OFFSET)
      .toEpochMilli
    TimeUtils.toEpochMilli(ldt.atOffset(TimeUtils.ZONE_CHINA_OFFSET)) mustBe
    ldt.toInstant(TimeUtils.ZONE_CHINA_OFFSET).toEpochMilli
    TimeUtils.toEpochMilli(ldt.atZone(TimeUtils.ZONE_CHINA_OFFSET)) mustBe
    ldt.toInstant(TimeUtils.ZONE_CHINA_OFFSET).toEpochMilli
  }

  test("testToLocalDate") {
    TimeUtils.toLocalDate("2019-04-01") mustBe LocalDate.of(2019, 4, 1)
    TimeUtils.toLocalDate("2019-04-1") mustBe LocalDate.of(2019, 4, 1)
    TimeUtils.toLocalDate("2019-4-1") mustBe LocalDate.of(2019, 4, 1)
    TimeUtils.toLocalDate("2019-4") mustBe LocalDate.of(2019, 4, 1)
  }

  test("testToSqlTime") {
    val sqlTime = TimeUtils.toSqlTime(LocalTime.of(7, 21, 59))
    val sqlTime1 = TimeUtils.toSqlTime("07:21:59")
    val sqlTime2 = TimeUtils.toSqlTime("7:21:59")
    sqlTime.toLocalTime mustBe sqlTime1.toLocalTime
    sqlTime.toLocalTime mustBe sqlTime2.toLocalTime
  }

  test("testToDayInt") {
    TimeUtils.toDayInt(LocalDate.of(2019, 12, 9)) mustBe 20191209
    TimeUtils.toDayInt(LocalDate.of(2019, 4, 9)) mustBe 20190409
  }

  test("testExecuteMillis") {
    val (_, millis) = TimeUtils.executeMillis(TimeUnit.MILLISECONDS.sleep(500))
    millis must be >= 500L
  }

  test("testExecuteNanoTime") {
    val (_, nanos) = TimeUtils.executeNanoTime(TimeUnit.NANOSECONDS.sleep(1500))
    nanos must be >= 1500L
  }
}
