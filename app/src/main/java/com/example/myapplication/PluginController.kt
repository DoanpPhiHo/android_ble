package com.example.myapplication

import android.bluetooth.BluetoothGatt
import android.content.Context

interface HandleBle {
    fun stateHandle(state: StateConnect, address: String)
    fun scanResultHandle(info: DeviceInfo)
}

class PluginController(
    private val handleBle: HandleBle,
    private val context: Context,
    private val bleClientCallback: IBleClientCallBack,
    private val callback: PermissionCallback,
    bleStatus: BleStatus,
) {
    private var bleClient: BleClient? = null

    init {
//        bleClient = BleClient(context, callback, this, bleStatus)
    }

    companion object {
        private var devices = arrayListOf<BluetoothGatt>()
    }

//    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
//    fun dispose() {
//        devices.forEach {
//            it.close()
//        }
//        devices = arrayListOf()
//    }

    //#endregion
    //#region ble

//    private fun disconnect(deviceId: String) {
//        Log.e(PluginController::class.simpleName, "disconnect: MethodChannel")
//
//        val check = devices.any { it.device.address == deviceId }
//        if (!check) return
//
//        disconnectBle(deviceId)
//    }

//    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
//    private fun writeCharacteristicAsync(
//        baseUUID: String,
//        charUUID: String,
//        deviceId: String,
//        value: List<Int>,
//        charUUIDNotification: String? = null
//    ) {
//        val check = devices.any { it.device.address == deviceId }
//        if (!check) {
//            throw  Exception("not found deviceId: $deviceId")
//        }
//        val gatt = devices.first { it.device.address == deviceId }
//        val data = bleClient?.writeCharacteristic(
//            gatt,
//            baseUUID,
//            charUUID,
//            value
//        ) ?: false
//        if (data) {
////            writeCharacteristicChannel.createRequest(result)
//            charUUIDNotification?.let {
//                handleSubscriber(deviceId, it, baseUUID)
//            }
//        } else {
//            // retry write
//            if (bleClient?.writeCharacteristic(
//                    gatt,
//                    baseUUID,
//                    charUUID,
//                    value
//                ) == true
//            ) {
////                writeCharacteristicChannel.createRequest(result)
//                charUUIDNotification?.let {
//                    handleSubscriber(deviceId, it, baseUUID)
//                }
//            } else {
//                throw  Exception("write data error")
//            }
//        }
//    }

//    private fun readCharacteristic(
//        serviceId: String,
//        characteristicId: String,
//        deviceId: String,
//    ) {
//        val gatt = devices.firstOrNull { it.device.address == deviceId } ?: return
//
//        bleClient?.readCharacteristic(gatt, serviceId, characteristicId)
////        readCharacteristicChannel.createRequest(result)
//    }

    //#endregion
}