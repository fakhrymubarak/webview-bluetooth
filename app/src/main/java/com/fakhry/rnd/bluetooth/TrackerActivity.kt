package com.fakhry.rnd.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.fakhry.rnd.bluetooth.databinding.ActivityWebViewBinding
import com.fakhry.rnd.bluetooth.thread.ConnectThread
import com.fakhry.rnd.bluetooth.utils.hasAllPermissions
import com.fakhry.rnd.bluetooth.utils.hasAtLeastOnePermissionOf
import com.fakhry.rnd.bluetooth.utils.hasPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.Manifest.permission as Permission

class TrackerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebViewBinding
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private var connectionThread: ConnectThread? = null

    // Store discovered Bluetooth devices
    private val discoveredDevices = mutableSetOf<String>()

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            when (action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> onDiscoveryStarted()
                BluetoothDevice.ACTION_FOUND -> onDeviceFound(intent)
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> onPairing(intent)
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> onDiscoveryFinished()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initWebView()
    }

    override fun onDestroy() {
        connectionThread?.cancel()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            BT_REQUEST_CODE -> handleBluetoothPermissionResult(grantResults)
            LOC_REQUEST_CODE -> handleLocationPermissionResult(grantResults)
        }
    }

    private fun initWebView() = with(binding.webView) {
        settings.javaScriptEnabled = true
        addJavascriptInterface(TrackerWebInterface(this@TrackerActivity), "Android")
        loadUrl("file:///android_asset/sample.html")

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript("javascript:setCustomerType('Prepaid');") { callback ->
                    callback?.takeIf { it.isNotBlank() }?.let { showToast(it) }
                }
            }
        }
        webChromeClient = WebChromeClient()
    }

    private fun onDiscoveryStarted() {
        showToast("Start Searching for Nearby Devices")
        discoveredDevices.clear()
    }

    private fun onDeviceFound(intent: Intent) {
        val device = IntentCompat.getParcelableExtra(
            intent,
            BluetoothDevice.EXTRA_DEVICE,
            BluetoothDevice::class.java
        )
        device?.let {
            if (hasPermission(Permission.BLUETOOTH_CONNECT)) {
                discoveredDevices.add(it.address)
                val name = it.name ?: return
                invokeJavascriptFunction("updateDeviceList('$name', '${it.address}')")
            } else {
                showToast("Permission BLUETOOTH_CONNECT not granted")
            }
        }
    }

    private fun onPairing(intent: Intent) {
        val device = IntentCompat.getParcelableExtra(
            intent,
            BluetoothDevice.EXTRA_DEVICE,
            BluetoothDevice::class.java
        )

        val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
        if (!hasPermission(Permission.BLUETOOTH_CONNECT)) return
        when (bondState) {
            BluetoothDevice.BOND_NONE -> showToast("Failed pairing with ${device?.name}")
            BluetoothDevice.BOND_BONDING -> showToast("Process pairing with ${device?.name}")
            BluetoothDevice.BOND_BONDED -> showToast("Success pairing with ${device?.name}")
        }
    }

    private fun onDiscoveryFinished() {
        val message = if (discoveredDevices.isEmpty()) "No Devices Found" else "Finished Searching for Nearby Devices"
        showToast(message)
    }

    fun requestBluetoothPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Permission.BLUETOOTH_CONNECT, Permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Permission.BLUETOOTH, Permission.BLUETOOTH_ADMIN)
        }
        ActivityCompat.requestPermissions(this, permissions, BT_REQUEST_CODE)
    }

    fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Permission.ACCESS_FINE_LOCATION, Permission.ACCESS_COARSE_LOCATION),
            LOC_REQUEST_CODE
        )
    }

    fun scanBluetooth() {
        val hasLocationPermission = hasAtLeastOnePermissionOf(
            Permission.ACCESS_FINE_LOCATION, Permission.ACCESS_COARSE_LOCATION
        )
        val hasBluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasAllPermissions(Permission.BLUETOOTH_CONNECT, Permission.BLUETOOTH_SCAN)
        } else {
            hasAllPermissions(Permission.BLUETOOTH, Permission.BLUETOOTH_ADMIN)

        }
        if (hasBluetoothPermission && hasLocationPermission) {
            if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
            bluetoothAdapter.startDiscovery()
            registerReceiver(bluetoothReceiver, IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            })
        } else {
            requestBluetoothPermission()
            showToast("Permission BLUETOOTH_SCAN && LOCATION not granted")
        }
    }

    fun connectTo(deviceAddress: String) {
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        if (device?.bondState != BluetoothDevice.BOND_BONDED) {
            device?.createBond()
        } else {
            showToast("This device has been connected")
        }
    }

    fun checkPairedBluetooth() {
        val hasLocationPermission = hasAtLeastOnePermissionOf(
            Permission.ACCESS_FINE_LOCATION, Permission.ACCESS_COARSE_LOCATION
        )
        if (hasPermission(Permission.BLUETOOTH_CONNECT) && hasLocationPermission) {
            lifecycleScope.launch(Dispatchers.Main) {
                bluetoothAdapter.bondedDevices?.forEach { device ->
                    val script = "updatePairedDeviceList('${device.name}', '${device.address}')"
                    invokeJavascriptFunction(script)
                }
            }
        } else {
            requestBluetoothPermission()
        }
    }

    private fun handleBluetoothPermissionResult(grantResults: IntArray) {
        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        val result = if (granted) "allow" else "denied"
        showToast(if (granted) "Bluetooth permission granted" else "Bluetooth permission denied")

        val script = "onBluetoothPermissionResult('$result')"
        invokeJavascriptFunction(script)
    }

    private fun handleLocationPermissionResult(grantResults: IntArray) {
        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        val result = if (granted) "allow" else "denied"
        showToast(if (granted) "Location permission granted" else "Location permission denied")

        val script = "onLocationPermissionResult('$result')"
        invokeJavascriptFunction(script)
    }

    private fun invokeJavascriptFunction(
        script: String,
        resultCallback: ValueCallback<String>? = null
    ) = binding.webView.evaluateJavascript("javascript:$script;", resultCallback)

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val BT_REQUEST_CODE = 1001
        private const val LOC_REQUEST_CODE = 1002
    }
}
