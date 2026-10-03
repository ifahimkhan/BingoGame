package com.fahim.bingonumbercaller.network

import java.net.Inet4Address
import java.net.NetworkInterface

actual object LocalIpProvider {
    actual fun getLocalIpAddress(): String? {
        return try {
            val candidates = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { iface -> runCatching { iface.isUp && !iface.isLoopback }.getOrDefault(false) }
                .flatMap { iface ->
                    iface.inetAddresses.toList()
                        .filterIsInstance<Inet4Address>()
                        .filter { !it.isLoopbackAddress }
                        .mapNotNull { addr ->
                            addr.hostAddress?.let { InterfaceAddress(iface.name, it, addr.isSiteLocalAddress) }
                        }
                }
            HostAddressPicker.pick(candidates)
        } catch (e: Exception) {
            null
        }
    }
}
