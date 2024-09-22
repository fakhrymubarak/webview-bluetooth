package com.fakhry.rnd.bluetooth.thread

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.widget.Toast
import java.io.IOException
import java.util.UUID

/**
 * This thread runs while attempting to make an outgoing connection
 * with a device. It runs straight through; the connection either
 * succeeds or fails.
 *
 * @param context The application context.
 * @param device The BluetoothDevice to connect to.
 * @param bluetoothAdapter The BluetoothAdapter to use for the connection.
 */
class ConnectThread(
    private val context: Context,
    private val device: BluetoothDevice,
    private val bluetoothAdapter: BluetoothAdapter,
) : Thread() {
    /**
     * Lazily initializes the BluetoothSocket. This is done using
     * `createRfcommSocketToServiceRecord` to create an insecure RFCOMM
     * (Radio Frequency Communication) socket.
     */
    private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID
        device.createRfcommSocketToServiceRecord(uuid)
    }

    /**
     * Starts the thread. Cancels Bluetooth discovery and attempts to connect
     * to the remote device through the socket.
     */
    override fun start() {
        // Cancel discovery because it otherwise slows down the connection.
        bluetoothAdapter.cancelDiscovery()

        mmSocket?.let { socket ->
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            try {
                socket.connect()
                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                // manageMyConnectedSocket(socket) // TODO: Handle the connected socket
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(context, "Could not connect to the device", Toast.LENGTH_SHORT)
                    .show()
                cancel()
            }
        }
    }

    /**
     * Closes the client socket and causes the thread to finish.
     */
    fun cancel() {
        try {
            mmSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Could not close the connect socket", Toast.LENGTH_SHORT).show()
        }
    }
}