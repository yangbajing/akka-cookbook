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

import java.io.InputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.{ HashMap => JHashMap }
import java.util.{ Map => JMap }

import scala.annotation.tailrec
import scala.collection.immutable
import scala.compat.java8.FunctionConverters.asJavaBiConsumer

object StringUtils {
  val BLACK_CHAR: Char = ' '
  val PRINTER_CHARS
      : immutable.IndexedSeq[Char] = ('0' to '9') ++ ('a' to 'z') ++ ('A' to 'Z')
  val REGEX_STR_BLANK = """[\s　]+"""
  val CHINESE_COMMA = "，"
  val CHINESE_FULL_STOP = "。"

  val PRINTER_CHARS_EXT: immutable.IndexedSeq[Char] = PRINTER_CHARS ++
    Vector('!', '#', '$', '%', '^', '&', '*', '(', ')', '-', '+', '=', '_', '.',
      '?', '<', '>')

  private val HEX_CHARS: Array[Char] = "0123456789abcdef".toCharArray
  private val HEX_CHAR_SETS = Set[Char]() ++ ('0' to '9') ++ ('a' to 'f') ++ ('A' to 'F')

  def blankToChineseComma(text: String, replacement: String): String =
    blankReplaceAll(text, CHINESE_COMMA)

  def blankReplaceAll(text: String, replacement: String): String =
    text.replaceAll(REGEX_STR_BLANK, replacement)

  def extractFirstName(msg: Any): Option[String] = msg match {
    case c: AnyRef =>
      val s = convertPropertyToUnderscore(c.getClass.getSimpleName)
      Some(s.take(s.indexOf('_')))
    case _ => None
  }

  def option(text: String): Option[String] =
    if (isBlank(text)) None else Some(text)

  def option(text: Option[String]): Option[String] = text.flatMap(option)

  @inline def isHex(c: Char): Boolean = HEX_CHAR_SETS.contains(c)

  /** Turns an array of Byte into a String representation in hexadecimal. */
  def hex2Str(bytes: Array[Byte]): String = {
    val hex = new Array[Char](2 * bytes.length)
    var i = 0
    while (i < bytes.length) {
      hex(2 * i) = HEX_CHARS((bytes(i) & 0xF0) >>> 4)
      hex(2 * i + 1) = HEX_CHARS(bytes(i) & 0x0F)
      i = i + 1
    }
    new String(hex)
  }

  /** Turns a hexadecimal String into an array of Byte. */
  def str2Hex(str: String): Array[Byte] = {
    val bytes = new Array[Byte](str.length / 2)
    var i = 0
    while (i < bytes.length) {
      bytes(i) = Integer.parseInt(str.substring(2 * i, 2 * i + 2), 16).toByte
      i += 1
    }
    bytes
  }

  def isEmpty(s: CharSequence): Boolean =
    (s eq null) || s.length() == 0

  @inline
  def isNoneEmpty(s: CharSequence): Boolean = !isEmpty(s)

  def isBlank(s: CharSequence): Boolean = {
    @tailrec
    def isNoneBlankChar(s: CharSequence, i: Int): Boolean =
      if (i < s.length()) s.charAt(i) != BLACK_CHAR || isNoneBlankChar(s, i + 1)
      else false

    isEmpty(s) || !isNoneBlankChar(s, 0)
  }

  @inline
  def isNoneBlank(s: CharSequence): Boolean = !isBlank(s)

  def randomString(n: Int): String = {
    val len = PRINTER_CHARS.length
    val sb = new StringBuilder
    var i = 0
    while (i < n) {
      i += 1
      val idx = Utils.random.nextInt(len)
      val c = PRINTER_CHARS.apply(idx)
      sb.append(c)
    }
    sb.toString()
  }

  def randomExtString(n: Int): String = {
    val len = PRINTER_CHARS_EXT.length
    val sb = new StringBuilder
    var i = 0
    while (i < n) {
      i += 1
      val idx = Utils.random.nextInt(len)
      val c = PRINTER_CHARS_EXT.apply(idx)
      sb.append(c)
    }
    sb.toString()
  }

  /**
   * 字符串从属性形式转换为下划线形式
   *
   * @param name    待转字符串
   * @param isLower 转换成下划线形式后是否使用小写，false将完全使用大写
   * @return 转换后字符串
   */
  def convertPropertyToUnderscore(name: String, isLower: Boolean = true): String =
    if (isBlank(name)) {
      name
    } else {
      val sb = new StringBuilder
      for (c <- name) {
        if (Character.isUpperCase(c)) {
          sb.append('_')
        }
        sb.append(
          if (isLower) Character.toLowerCase(c)
          else Character.toUpperCase(c.toUpper))
      }
      sb.toString()
    }

  def toUnderscore(name: String, isLower: Boolean = true): String =
    if (isBlank(name)) {
      name
    } else {
      val sb = new StringBuilder
      for (c <- name) {
        if (Character.isUpperCase(c)) {
          sb.append('_')
        }
        if ('-' == c) {
          sb.append('_')
        } else {
          sb.append(
            if (isLower) Character.toLowerCase(c)
            else Character.toUpperCase(c.toUpper))
        }
      }
      sb.toString()
    }

  /**
   * Convert a column name with underscores to the corresponding property name using "camel case".  A name
   * like "customer_number" would match a "customerNumber" property name.
   *
   * @param name the column name to be converted
   * @return the name using "camel case"
   */
  def convertUnderscoreToProperty(name: String): String =
    convertToProperty(name, '_')

  def convertStrikethroughToProperty(name: String): String =
    convertToProperty(name, '-')

  def convertToProperty(name: String, ch: Char): String =
    if (isBlank(name)) {
      ""
    } else {
      val arr = name.split(ch)
      arr.head + arr.tail.map(item => item.head.toUpper + item.tail).mkString
    }

  def toProperty(name: String): String =
    if (isBlank(name)) {
      ""
    } else {
      val arr = name.split("""[_-]""")
      arr.head + arr.tail.map(item => item.head.toUpper + item.tail).mkString
    }

  def convertUnderscoreNameToPropertyName(obj: Map[String, Any]): Map[String, Any] =
    obj.map {
      case (key, value) => convertUnderscoreNameToPropertyName(key) -> value
    }

  def convertUnderscoreNameToPropertyName(
      obj: JMap[String, Object]): JMap[String, Object] = {
    val result = new JHashMap[String, Object]()
    val func: (String, Object) => Unit = (key, value) =>
      result.put(convertUnderscoreNameToPropertyName(key), value)
    obj.forEach(asJavaBiConsumer(func))
    result
  }

  def convertUnderscoreNameToPropertyName(name: String): String = {
    val result = new StringBuilder
    var nextIsUpper = false
    if (name != null && name.length > 0) {
      if (name.length > 1 && name.substring(1, 2) == "_") {
        result.append(name.substring(0, 1).toUpperCase)
      } else {
        result.append(name.substring(0, 1).toLowerCase)
      }

      var i = 1
      val len = name.length
      while (i < len) {
        val s = name.substring(i, i + 1)
        if (s == "_") {
          nextIsUpper = true
        } else if (nextIsUpper) {
          result.append(s.toUpperCase)
          nextIsUpper = false
        } else {
          result.append(s.toLowerCase)
        }

        i += 1
      }
    }
    result.toString
  }

  def snakeCaseToCamelCase(name: String, upperInitial: Boolean = false): String = {
    val b = new StringBuilder()
    @tailrec
    def inner(name: String, index: Int, capNext: Boolean): Unit =
      if (name.nonEmpty) {
        val (r, capNext2) = name.head match {
          case c if c.isLower => (Some(if (capNext) c.toUpper else c), false)
          case c if c.isUpper =>
            // force first letter to lower unless forced to capitalize it.
            (Some(if (index == 0 && !capNext) c.toLower else c), false)
          case c if c.isDigit => (Some(c), true)
          case _              => (None, true)
        }
        r.foreach(b.append)
        inner(name.tail, index + 1, capNext2)
      }
    inner(name, 0, upperInitial)
    b.toString
  }

  /**
   * Check that the given {@code CharSequence} is neither {@code null} nor
   * of length 0.
   * <p>Note: this method returns {@code true} for a {@code CharSequence}
   * that purely consists of whitespace.
   * <p><pre class="code">
   * StringUtils.hasLength(null) = false
   * StringUtils.hasLength("") = false
   * StringUtils.hasLength(" ") = true
   * StringUtils.hasLength("Hello") = true
   * </pre>
   *
   * @param str the {@code CharSequence} to check (may be {@code null})
   * @return {@code true} if the {@code CharSequence} is not {@code null} and has length
   * @see #hasText(String)
   */
  def hasLength(str: CharSequence): Boolean = { str != null && str.length > 0 }

  /**
   * Check that the given {@code String} is neither {@code null} nor of length 0.
   * <p>Note: this method returns {@code true} for a {@code String} that
   * purely consists of whitespace.
   *
   * @param str the {@code String} to check (may be {@code null})
   * @return {@code true} if the {@code String} is not {@code null} and has length
   * @see #hasLength(CharSequence)
   * @see #hasText(String)
   */
  def hasLength(str: String): Boolean =
    hasLength(str.asInstanceOf[CharSequence])

  /**
   * Trim <i>all</i> whitespace from the given {@code String}:
   * leading, trailing, and in between characters.
   *
   * @param str the {@code String} to check
   * @return the trimmed {@code String}
   * @see Character#isWhitespace
   */
  def trimAllWhitespace(str: String): String = {
    if (!hasLength(str)) {
      return str
    }
    val len = str.length
    val sb = new StringBuilder(str.length)
    var i = 0
    while ({ i < len }) {
      val c = str.charAt(i)
      if (!Character.isWhitespace(c)) {
        sb.append(c)
      }

      { i += 1; i - 1 }
    }
    sb.toString
  }

  def bytesToHex(bytes: Array[Byte]): Array[Char] = {
    val hexChars = new Array[Char](bytes.length * 2)
    var j = 0
    while (j < bytes.length) {
      val v = bytes(j) & 0xFF
      hexChars(j * 2) = HEX_CHARS(v >>> 4)
      hexChars(j * 2 + 1) = HEX_CHARS(v & 0x0F)
      j += 1
    }
    hexChars
  }

  @inline def toHexString(arr: Array[Byte]): String =
    new String(bytesToHex(arr))

  def fromByteArray(bytes: Array[Byte]) = new String(asCharArray(bytes))

  def asCharArray(bytes: Array[Byte]): Array[Char] = {
    val chars = new Array[Char](bytes.length)
    var i = 0
    while (i != chars.length) {
      chars(i) = (bytes(i) & 0xff).toChar
      i += 1
    }
    chars
  }

  def readLineIterator(is: InputStream): Iterator[String] = {
    val rs = scala.io.Source.fromInputStream(is)
    try {
      rs.getLines()
    } finally {
      rs.close()
    }
  }
  def toString(is: InputStream): String = readLineIterator(is).mkString

  def toString(e: Throwable): String = {
    val pw = new PrintWriter(new StringWriter())
    try {
      e.printStackTrace(pw)
      pw.toString
    } finally {
      Utils.closeQuiet(pw)
    }
  }

  /**
   * 从目录读取所有文件的所有行，并过滤掉空行（包括空白字符行）
   *
   * @param dir 目录
   * @return
   */
  def readAllLinesFromPath(dir: Path): java.util.stream.Stream[String] = {
    import scala.compat.java8.FunctionConverters._
    val filterNoneBlank: String => Boolean = s => StringUtils.isNoneBlank(s)
    val trim: String => String = s => s.trim
    val trans: Path => java.util.stream.Stream[String] = path =>
      Files.readAllLines(path).stream()
    Files
      .list(dir)
      .flatMap(trans.asJava)
      .map[String](trim.asJava)
      .filter(filterNoneBlank.asJava)
  }
}
