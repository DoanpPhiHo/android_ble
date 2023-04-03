package com.example.myapplication

interface PermissionCallback {
    fun requestPermission()
    fun requestEnableLocation()
    fun enableBluetooth()
}