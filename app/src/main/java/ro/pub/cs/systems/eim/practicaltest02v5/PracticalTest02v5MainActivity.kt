package ro.pub.cs.systems.eim.practicaltest02v5

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ro.pub.cs.systems.eim.practicaltest02v5.R
import ro.pub.cs.systems.eim.practicaltest02v5.Constants
import ro.pub.cs.systems.eim.practicaltest02v5.ClientThread
import ro.pub.cs.systems.eim.practicaltest02v5.ServerThread

class PracticalTest02v5MainActivity : AppCompatActivity() {

    private lateinit var serverPortEditText: EditText
    private lateinit var clientAddressEditText: EditText
    private lateinit var clientPortEditText: EditText
    private lateinit var requestEditText: EditText
    private lateinit var responseTextView: TextView

    private var serverThread: ServerThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(Constants.TAG, "[MAIN ACTIVITY] onCreate()")
        setContentView(R.layout.activity_practical_test02v5_main)

        serverPortEditText = findViewById(R.id.server_port_edit_text)
        clientAddressEditText = findViewById(R.id.client_address_edit_text)
        clientPortEditText = findViewById(R.id.client_port_edit_text)
        requestEditText = findViewById(R.id.request_edit_text)
        responseTextView = findViewById(R.id.response_text_view)

        findViewById<Button>(R.id.connect_button).setOnClickListener {
            startServer()
        }

        findViewById<Button>(R.id.send_button).setOnClickListener {
            sendRequest()
        }
    }

    private fun startServer() {
        val serverPortStr = serverPortEditText.text.toString().trim()
        if (serverPortStr.isEmpty()) {
            Toast.makeText(this, "Server port required", Toast.LENGTH_SHORT).show()
            return
        }

        val port = try {
            serverPortStr.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Server port must be number", Toast.LENGTH_SHORT).show()
            return
        }

        serverThread = ServerThread(port)
        if (serverThread?.getServerSocket() == null) {
            Toast.makeText(this, "Could not start server", Toast.LENGTH_SHORT).show()
            return
        }

        serverThread?.start()
        Toast.makeText(this, "Server started on $port", Toast.LENGTH_SHORT).show()
    }

    private fun sendRequest() {
        val clientAddress = clientAddressEditText.text.toString().trim()
        val clientPortStr = clientPortEditText.text.toString().trim()

        if (clientAddress.isEmpty() || clientPortStr.isEmpty()) {
            Toast.makeText(this, "Client address/port required", Toast.LENGTH_SHORT).show()
            return
        }

        val clientPort = try {
            clientPortStr.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Client port must be number", Toast.LENGTH_SHORT).show()
            return
        }

        if (serverThread == null || serverThread?.isAlive != true) {
            Toast.makeText(this, "No server running", Toast.LENGTH_SHORT).show()
            return
        }

        val raw = requestEditText.text.toString()
        val lines = raw.split(Regex("\\r?\\n"))

        if (lines.size < 2) {
            Toast.makeText(
                this,
                "Format:\nget\nKEY\nor\nput\nKEY\nVALUE",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val op = lines[0].trim().lowercase()
        val key = lines[1].trim()
        val value = if (lines.size >= 3) lines[2] else Constants.EMPTY_STRING

        if (op.isEmpty() || key.isEmpty()) {
            Toast.makeText(this, "Operation and key required", Toast.LENGTH_SHORT).show()
            return
        }

        responseTextView.text = Constants.EMPTY_STRING

        ClientThread(
            clientAddress,
            clientPort,
            op,
            key,
            value,
            responseTextView
        ).start()
    }

    override fun onDestroy() {
        Log.i(Constants.TAG, "[MAIN ACTIVITY] onDestroy()")
        serverThread?.stopThread()
        super.onDestroy()
    }
}
