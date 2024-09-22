package com.fakhry.rnd.bluetooth.utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Checks if the context has the specified permission granted.
 *
 * @param permission The permission to check.
 * @return`true` if the permission is granted, `false` otherwise.
 */
fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Checks if the context has all of the specified permissions granted.
 *
 * @param permissions The permissions to check.
 *@return `true` if all permissions are granted, `false` otherwise.
 */
fun Context.hasAllPermissions(vararg permissions: String): Boolean {
    return permissions.all {
        ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Checks if the context has at least one of the specified permissions granted.
 *
 * @param permissions The permissions to check.
 * @return `true` if at least one permission is granted, `false` otherwise.
 */
fun Context.hasAtLeastOnePermissionOf(vararg permissions: String): Boolean {
    return permissions.any {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
}