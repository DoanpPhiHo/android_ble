package com.example.myapplication

import android.bluetooth.BluetoothGatt
import android.content.Context
import android.os.Build

interface IBleFunction {

    fun startScan(serviceUuids: List<String>)

    fun stopScan()

    fun subscriber(gatt: BluetoothGatt, characteristicId: String, baseUUID: String): Boolean

    fun connect(deviceId: String): BluetoothGatt

    fun disconnect(gatt: BluetoothGatt?)

    fun readCharacteristic(
        gatt: BluetoothGatt,
        serviceId: String,
        characteristicId: String
    ): Boolean

    fun writeCharacteristic(
        gatt: BluetoothGatt,
        serviceId: String,
        characteristicId: String,
        value: List<Int>,
    ): Boolean

    fun onActivityResult(requestCode: Int, resultCode: Int)
}