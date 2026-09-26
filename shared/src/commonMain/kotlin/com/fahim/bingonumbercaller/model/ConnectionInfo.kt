package com.fahim.bingonumbercaller.model

import kotlinx.serialization.Serializable

@Serializable
data class ConnectionInfo(
    val host: String,
    val port: Int,
    val sessionToken: String
) {
    fun toPayloadString(): String =
        "bingolive://connect?host=$host&port=$port&token=$sessionToken"

    companion object {
        fun parse(payload: String): ConnectionInfo? {
            try {
                var trimmed = payload.trim()
                if (trimmed.isEmpty()) return null

                // Strip surrounding quotes if present
                if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
                    (trimmed.startsWith("'") && trimmed.endsWith("'"))
                ) {
                    trimmed = trimmed.substring(1, trimmed.length - 1).trim()
                }

                // 1. Try parsing query parameters if query string is present (e.g. ?host=...&port=...)
                if (trimmed.contains("?")) {
                    val queryString = trimmed.substringAfter("?")
                    val params = queryString.split("&").associate { param ->
                        val parts = param.split("=", limit = 2)
                        if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to ""
                    }
                    val queryHost = params["host"]?.takeIf { it.isNotEmpty() }
                    val queryPort = params["port"]?.toIntOrNull()
                    val token = params["token"] ?: ""
                    if (queryHost != null && queryPort != null && queryPort in 1..65535) {
                        return ConnectionInfo(host = queryHost, port = queryPort, sessionToken = token)
                    }
                }

                // 2. Try parsing standard URI/URL format (e.g. http://host:port, ws://host:port, bingolive://...)
                var withoutScheme = trimmed
                if (withoutScheme.contains("://")) {
                    withoutScheme = withoutScheme.substringAfter("://")
                }

                // Extract token from query if present
                var extractedToken = ""
                if (withoutScheme.contains("?")) {
                    val query = withoutScheme.substringAfter("?")
                    withoutScheme = withoutScheme.substringBefore("?")
                    val params = query.split("&").associate { param ->
                        val parts = param.split("=", limit = 2)
                        if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to ""
                    }
                    extractedToken = params["token"] ?: ""
                }

                // Check path segments for host:port (e.g. 192.168.1.5:8080/game)
                val segments = withoutScheme.split("/")
                for (segment in segments) {
                    val seg = segment.trim()
                    if (seg.contains(":")) {
                        val colonParts = seg.split(":")
                        if (colonParts.size >= 2) {
                            val h = colonParts[0].trim().removePrefix("[").removeSuffix("]")
                            val p = colonParts[1].toIntOrNull()
                            val t = if (colonParts.size > 2) colonParts[2].trim() else extractedToken
                            if (h.isNotEmpty() && p != null && p in 1..65535) {
                                return ConnectionInfo(host = h, port = p, sessionToken = t)
                            }
                        }
                    }
                }

                // 3. Fallback: plain host:port or host:port:token
                if (trimmed.contains(":")) {
                    val colonParts = trimmed.split(":")
                    if (colonParts.size >= 2) {
                        val h = colonParts[0].trim().substringAfterLast("/").removePrefix("[").removeSuffix("]")
                        val portStr = colonParts[1].substringBefore("/").substringBefore("?").trim()
                        val p = portStr.toIntOrNull()
                        val t = if (colonParts.size > 2) colonParts[2].trim() else ""
                        if (h.isNotEmpty() && p != null && p in 1..65535) {
                            return ConnectionInfo(host = h, port = p, sessionToken = t)
                        }
                    }
                }

                return null
            } catch (e: Exception) {
                return null
            }
        }
    }
}

fun parseConnectionInfo(payload: String): ConnectionInfo? = ConnectionInfo.parse(payload)
