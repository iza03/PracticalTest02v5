package ro.pub.cs.systems.eim.practicaltest02v5

import android.util.Log
import org.json.JSONObject
import ro.pub.cs.systems.eim.practicaltest02v5.Constants
import ro.pub.cs.systems.eim.practicaltest02v5.Utilities
import java.io.BufferedReader
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

class CommunicationThread(
    private val serverThread: ServerThread,
    private val socket: Socket?
) : Thread() {

    override fun run() {
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!")
            return
        }

        try {
            val br: BufferedReader = Utilities.getReader(socket)
            val pw: PrintWriter = Utilities.getWriter(socket)

            var operation: String? = br.readLine()
            var key: String? = br.readLine()
            val value: String? = br.readLine()

            if (operation.isNullOrBlank() || key.isNullOrBlank()) {
                pw.println("ERROR: operation/key missing")
                pw.flush()
                return
            }

            operation = operation.trim().lowercase()
            key = key.trim()

            if (operation == Constants.OP_PUT) {
                serverThread.put(key, value ?: Constants.EMPTY_STRING)
                pw.println("OK (stored key=$key)")
                pw.flush()
                return
            }

            if (operation == Constants.OP_GET) {
                val cached = serverThread.get(key)
                if (cached != null) {
                    pw.println("CACHE HIT")
                    pw.println(cached)
                    pw.flush()
                    return
                }

                val fetched = fetchFromTimeNow(key)
                if (fetched == null) {
                    pw.println("ERROR: could not fetch timezone for key=$key")
                } else {
                    // optional: cache result
                    serverThread.put(key, fetched)
                    pw.println("CACHE MISS -> FETCHED")
                    pw.println(fetched)
                }
                pw.flush()
                return
            }

            pw.println("ERROR: unknown operation (use get / put)")
            pw.flush()

        } catch (e: Exception) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] ${e.message}")
        } finally {
            try {
                socket.close()
            } catch (ignored: IOException) {
            }
        }
    }

    private fun fetchFromTimeNow(key: String): String? {
        var conn: HttpsURLConnection? = null
        try {
            val safePath = buildSafeTimezonePath(key)
            val url = URL(Constants.TIME_NOW_BASE + safePath)

            conn = url.openConnection() as HttpsURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode != 200) {
                return null
            }

            val sb = StringBuilder()
            conn.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { sb.append(it) }
            }

            val obj = JSONObject(sb.toString())

            val timezone = obj.optString("timezone", key)
            val datetime = obj.optString("datetime", "")
            val utcOffset = obj.optString("utc_offset", "")
            val dst = obj.optString("dst", "")

            return """
                timezone=$timezone
                datetime=$datetime
                utc_offset=$utcOffset
                dst=$dst
            """.trimIndent()

        } catch (e: Exception) {
            Log.e(Constants.TAG, "[FETCH] ${e.message}")
            return null
        } finally {
            conn?.disconnect()
        }
    }

    @Throws(Exception::class)
    private fun buildSafeTimezonePath(key: String): String {
        val parts = key.split("/")
        val out = StringBuilder()

        for (part in parts) {
            if (part.isEmpty()) continue
            if (out.isNotEmpty()) out.append("/")
            out.append(URLEncoder.encode(part, "UTF-8"))
        }
        return out.toString()
    }
}
