package ro.pub.cs.systems.eim.practicaltest02v5
import android.util.Log
import ro.pub.cs.systems.eim.practicaltest02v5.Constants
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.HashMap

class ServerThread(port: Int) : Thread() {

    private var serverSocket: ServerSocket? = null

    // valoare + momentul când a fost pusă
    private val data: HashMap<String, Pair<String, Long>> = HashMap()

    private val TIMEOUT = 5000L // 5 secunde

    init {
        try {
            serverSocket = ServerSocket(port)
        } catch (e: IOException) {
            Log.e(Constants.TAG, "[SERVER THREAD] ${e.message}")
        }
    }

    fun getServerSocket(): ServerSocket? = serverSocket

    @Synchronized
    fun put(key: String, value: String) {
        data[key] = Pair(value, System.currentTimeMillis())
    }

    @Synchronized
    fun get(key: String): String? {
        val entry = data[key] ?: return null

        val value = entry.first
        val timestamp = entry.second
        val now = System.currentTimeMillis()

        return if (now - timestamp <= TIMEOUT) {
            value
        } else {
            // a expirat
            data.remove(key)
            null
        }
    }

    override fun run() {
        try {
            while (!currentThread().isInterrupted) {
                Log.i(Constants.TAG, "[SERVER THREAD] Waiting for client...")
                val socket: Socket = serverSocket?.accept() ?: break
                CommunicationThread(this, socket).start()
            }
        } catch (e: IOException) {
            Log.e(Constants.TAG, "[SERVER THREAD] ${e.message}")
        }
    }

    fun stopThread() {
        interrupt()
        try {
            serverSocket?.close()
        } catch (ignored: IOException) {
        }
    }
}
