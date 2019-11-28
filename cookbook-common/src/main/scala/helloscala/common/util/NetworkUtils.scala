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

import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface

import scala.jdk.CollectionConverters._

object NetworkUtils {
  private val validNetworkNamePrefixes = List("eth", "enp", "wlp")
  def validNetworkName(name: String) =
    validNetworkNamePrefixes.exists(prefix => name.startsWith(prefix))

  def interfaces(): Vector[NetworkInterface] =
    NetworkInterface.getNetworkInterfaces.asScala.toVector

  def onlineNetworkInterfaces() = {
    interfaces().filterNot(ni =>
      ni.isLoopback || !ni.isUp || ni.isVirtual || ni.isPointToPoint || !validNetworkName(ni.getName))
  }

  def onlineInterfaceAddress(): Vector[InterfaceAddress] = {
    onlineNetworkInterfaces().flatMap(i => i.getInterfaceAddresses.asScala)
  }

  def firstOnlineInet4Address(): Option[InetAddress] = {
    onlineInterfaceAddress().view.filter(ia => ia.getAddress.isInstanceOf[Inet4Address]).map(_.getAddress).headOption
  }

  def toInetSocketAddress(address: String, defaultPort: Int): InetSocketAddress =
    address.split(':') match {
      case Array(host, AsInt(port)) =>
        InetSocketAddress.createUnresolved(host, port)
      case Array(host) => InetSocketAddress.createUnresolved(host, defaultPort)
      case _           => throw new ExceptionInInitializerError(s"无效的通信地址：$address")
    }
}
