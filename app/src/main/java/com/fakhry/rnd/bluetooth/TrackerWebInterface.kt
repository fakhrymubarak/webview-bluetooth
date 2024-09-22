package com.fakhry.rnd.bluetooth

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.fakhry.rnd.bluetooth.utils.hasPermission
import kotlin.random.Random

/**
 * A class that provides an interface between a WebView and native Android code.
 * This class exposes methods to the WebView that allow it to interact with
 * Bluetooth and location services, display toasts, and open system settings.
 *
 * @param context The application context.
 */
class TrackerWebInterface(
    private val context: Context
) {
    /**
     * Displays a toast message with a random number appended.
     *
     * @param message The message to display in the toast.
     * @return A string indicating that the toast was shown and the random number.
     */
    @JavascriptInterface
    fun showToast(message: String): String {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        val random = Random.nextInt(0, 100)
        return "Show Toast - $random"
    }

    /**
     * Opens the application details settings for the current app.
     */
    @JavascriptInterface
    fun openPermissionSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    /**
     * Opens the Bluetooth settings.
     */
    @JavascriptInterface
    fun openBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        context.startActivity(intent)
    }

    /**
     * Checks if the app has Bluetooth permission.
     *
     * @return true if the permission is granted, false otherwise.
     */
    @JavascriptInterface
    fun hasBluetoothPermission(): Boolean {
        val activity = context as TrackerActivity
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12 and above
            activity.hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            // For Android 11 and below
            activity.hasPermission(Manifest.permission.BLUETOOTH)
        }
    }

    /**
     * Requests the BLUETOOTH_CONNECT permission.
     * The result is sent asynchronously to the WebView via
     * `evaluateJavascript`: onBluetoothPermissionResult(result).
     */
    @JavascriptInterface
    fun requestBluetoothPermission() {
        val activity = context as TrackerActivity
        activity.requestBluetoothPermission()
    }

    /**
     * Checks the status of location permissions.
     *
     * @return "allow" if location permission is granted permanently,
     *         "allow_once" if granted only once,
     *         "denied" if denied or not yet granted.
     */
    @JavascriptInterface
    fun checkLocationPermission(): String {
        val activity = context as TrackerActivity
        return if (activity.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            activity.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) ||
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                "allow_once"
            } else {
                "allow"
            }
        } else {
            "denied"
        }
    }

    /**
     * Requests ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permissions.
     * The result is sent asynchronously to the WebView via
     * `evaluateJavascript`: onLocationPermissionResult(result)
     */
    @JavascriptInterface
    fun requestLocationPermission() {
        val activity = context as TrackerActivity
        activity.requestLocationPermission()
    }

    /**
     * Scans for nearby Bluetooth devices.
     * Requires BLUETOOTH_CONNECT, BLUETOOTH_SCAN,ACCESS_FINE_LOCATION,
     * and ACCESS_COARSE_LOCATION permissions.
     *
     * Sends the device information asynchronously to the WebView multiple times
     * via `evaluateJavascript`: updateDeviceList(deviceName, deviceAddress).
     */
    @JavascriptInterface
    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ]
    )
    fun scanBluetooth() {
        val activity = context as TrackerActivity
        activity.scanBluetooth()
    }

    /**
     * Connects to a Bluetooth device with the given address.
     * Requires BLUETOOTH_CONNECT permission.
     *
     * @param deviceAddress The MAC address of the device to connect to.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @JavascriptInterface
    fun connectTo(deviceAddress: String) {
        val activity = context as TrackerActivity
        activity.connectTo(deviceAddress)
    }

    /**
     * Checks for paired Bluetooth devices.
     * Requires BLUETOOTH_CONNECT permission.
     *
     * Sends the device information asynchronously to the WebView multiple times
     * via `evaluateJavascript`: updatePairedDeviceList(deviceName, deviceAddress).
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @JavascriptInterface
    fun checkPairedBluetooth() {
        val activity = context as TrackerActivity
        activity.checkPairedBluetooth()
    }
}