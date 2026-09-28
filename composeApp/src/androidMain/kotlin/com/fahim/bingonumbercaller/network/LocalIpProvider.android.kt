package com.fahim.bingonumbercaller.network

import java.net.Inet4Address
import java.net.NetworkInterface

actual object LocalIpProvider {
    actual fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return null

            val wifiOrApCandidates = mutableListOf<String>()
            val siteLocalCandidates = mutableListOf<String>()
            val otherCandidates = mutableListOf<String>()

            for (iface in interfaces) {
                try {
                    if (iface.isLoopback || !iface.isUp) continue
                } catch (e: Exception) {
                    continue
                }

                val name = iface.name.lowercase()
                val isCellularOrVirtual = name.contains("rmnet") || name.contains("ccmni") ||
                        name.contains("dummy") || name.contains("tun") || name.contains("ppp")

                val isWireless = name.contains("wlan") || name.contains("ap") ||
                        name.contains("softap") || name.contains("swlan") ||
                        name.contains("tether") || name.contains("p2p")

                val addresses = iface.inetAddresses
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddr = addr.hostAddress ?: continue
                        if (addr.isSiteLocalAddress) {
                            if (isWireless) {
                                wifiOrApCandidates.add(hostAddr)
                            } else if (!isCellularOrVirtual) {
                                siteLocalCandidates.add(hostAddr)
                            } else {
                                otherCandidates.add(hostAddr)
                            }
                        } else if (!isCellularOrVirtual) {
                            otherCandidates.add(hostAddr)
                        }
                    }
                }
            }

            return wifiOrApCandidates.firstOrNull()
                ?: siteLocalCandidates.firstOrNull()
                ?: otherCandidates.firstOrNull()
        } catch (e: Exception) {
            return null
        }
    }
}
