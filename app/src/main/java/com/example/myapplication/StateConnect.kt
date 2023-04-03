package com.example.myapplication

import android.bluetooth.BluetoothGatt

enum class StateConnect {
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    DISCONNECTED,
}

enum class StatusBluetooth {
    TURING_ON,
    ON,
    TURING_OFF,
    OFF,
    NOT_SUPPORT,
}

data class StateData(val state: Int, val deviceId: String?)

data class DeviceInfo(val name: String, val id: String, var gatt: BluetoothGatt? = null)