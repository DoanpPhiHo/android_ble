package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat

class FunctionUtils {
    companion object {
        fun checkPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) checkPermissionS(context)
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) checkPermissionM(context)
            else checkPermissionLOLIPOD(context)
        }

        @RequiresApi(Build.VERSION_CODES.S)
        private fun checkPermissionS(context: Context): Boolean {
            return context.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
                    && context.checkSelfPermission(
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        }

        @RequiresApi(Build.VERSION_CODES.M)
        private fun checkPermissionM(context: Context): Boolean {
            return context.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        private fun checkPermissionLOLIPOD(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun requestPermission(activity: Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissionSdkS(activity)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissionM(activity)
            } else {
                requestPermissionLOLLIPOP(activity)
            }
        }

        @RequiresApi(Build.VERSION_CODES.S)
        private fun requestPermissionSdkS(activity: Activity) {
            activity.requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), 99
            )
        }

        @RequiresApi(Build.VERSION_CODES.M)
        private fun requestPermissionM(activity: Activity) {
            activity.requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                99
            )
        }

        private fun requestPermissionLOLLIPOP(activity: Activity) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), 99
            )
        }

        fun getManager(context: Context): BluetoothManager? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getSystemService(BluetoothManager::class.java)
            } else {
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
            }
        }

        fun isLocationEnabled(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val lm: LocationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                lm.isLocationEnabled
            } else {
                isLocationEnabled21(context)
            }
        }

        @Suppress("DEPRECATION")
        private fun isLocationEnabled21(context: Context): Boolean {
            val mode: Int =
                Settings.Secure.getInt(
                    context.contentResolver, Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF
                )
            return mode != Settings.Secure.LOCATION_MODE_OFF
        }
    }
}