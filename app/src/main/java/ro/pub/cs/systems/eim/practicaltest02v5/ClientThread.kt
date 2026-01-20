package ro.pub.cs.systems.eim.practicaltest02v5

import android.util.Log
import android.widget.TextView
import ro.pub.cs.systems.eim.practicaltest02v5.Constants
import ro.pub.cs.systems.eim.practicaltest02v5.Utilities
import java.io.BufferedReader
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket

class ClientThread(
    private val address: String,
    private val port: Int,
    private val operation: String,
    private val key: String,
    private val value: String?,
    private val responseTextView: TextView
) : Thread() {

    private var socket: Socket? = null

    override fun run() {
        try {
            socket = Socket(address, port)

            val bufferedReader: BufferedReader = Utilities.getReader(socket!!)
            val printWriter: PrintWriter = Utilities.getWriter(socket!!)

            printWriter.println(operation)
            printWriter.println(key)
            printWriter.println(value ?: Constants.EMPTY_STRING)
            printWriter.flush()

            val sb = StringBuilder()
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }

            val response = sb.toString().trim()
            responseTextView.post {
                responseTextView.text = response
            }

        } catch (e: IOException) {
            Log.e(Constants.TAG, "[CLIENT THREAD] ${e.message}")
            responseTextView.post {
                responseTextView.text = "Client error: ${e.message}"
            }
        } finally {
            try {
                socket?.close()
            } catch (ignored: IOException) {
            }
        }
    }
}
