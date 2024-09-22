package com.fakhry.rnd.bluetooth.thread

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.widget.Toast
import java.io.IOException
import java.util.UUID

/**
 * This thread runs while listening for incoming connections. It behaves
 * like a server-side client. It runs until a connection is accepted
 * (or until cancelled).
 *
 * @param context The application context.
 * @param bluetoothAdapter The BluetoothAdapter to use for accepting connections.
 */
class AcceptThread(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter,
) : Thread() {/**
 * Lazily initializes the BluetoothServerSocket. This is done using
 * `listenUsingInsecureRfcommWithServiceRecord` to create an insecure RFCOMM
 * (Radio Frequency Communication) socket.
 */
private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
    val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID
    bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("RnD", uuid)
}

    /**
     * Runs the thread. Continuously tries to accept incoming connections until
     * an exception occurs or a socket is returned.
     */
    override fun run() {
        var shouldLoop = true
        while (shouldLoop) {
            val socket: BluetoothSocket? = try {
                mmServerSocket?.accept()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context, "Socket's accept() method failed", Toast.LENGTH_SHORT).show()
                shouldLoop = false
                null
            }
            socket?.also {
                // manageMyConnectedSocket(it) // TODO: Handle the connected socket
                mmServerSocket?.close()
                shouldLoop = false
            }
        }
    }

    /**
     * Cancels the listening socket and stops the thread.
     */
    fun cancel() {
        try {
            mmServerSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Could not close the connect socket", Toast.LENGTH_SHORT).show()
        }
    }
}