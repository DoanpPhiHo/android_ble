package com.example.myapplication

import android.bluetooth.le.ScanResult

interface IBleClientCallBack {
    fun onScanResult(result: ScanResult)
    fun deviceState(id: String, name: String?, state: StateConnect)
    fun onCharacteristicRead(id: String, characteristicId: String, value: ByteArray)
    fun onCharacteristicChanged(id: String, characteristicId: String, value: ByteArray)
    fun onBonded(bonded: Boolean)

    // == BluetoothGatt#GATT_SUCCESS
    fun onServicesDiscovered(status: Boolean)
}