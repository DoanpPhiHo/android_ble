package com.example.myapplication

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.lang.reflect.Method
import java.util.*

interface BleStatus {
    fun onStatus(status: StatusBluetooth)
}

class BleClient(
    private val context: Context,
    private val permissionCallback: PermissionCallback,
    val bleClientCallBack: IBleClientCallBack,
    val bleStatus: BleStatus,
) :
    IBleFunction {
    @Suppress("PropertyName")
    val TAG = BleClient::class.simpleName

    private lateinit var adapter: BluetoothAdapter

    private lateinit var scanner: BluetoothLeScanner
    private var isScanInProgress = false

    init {
        if (!FunctionUtils.isLocationEnabled(context)) {
            permissionCallback.requestEnableLocation()
            permissionCallback.enableBluetooth()
            permissionCallback.requestPermission()
        }
        initBle()
    }

    private fun initBle() {
        val manager = FunctionUtils.getManager(context)
        if (manager?.adapter == null) {
            bleStatus.onStatus(StatusBluetooth.NOT_SUPPORT)
            throw Exception("Ble not support")
        } else {
            if (manager.adapter.bluetoothLeScanner != null) {
                adapter = manager.adapter
                scanner = adapter.bluetoothLeScanner
                bleStatus.onStatus(if (isEnable()) StatusBluetooth.ON else StatusBluetooth.OFF)
            } else {
                permissionCallback.enableBluetooth()
            }
        }
    }

    private fun isEnable(): Boolean {
        return adapter.isEnabled
    }

    //#region ble setup
    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (!isScanInProgress) return
            result?.let {
                bleClientCallBack.onScanResult(it)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> {
                    Log.e(TAG, "onScanFailed: SCAN_FAILED_ALREADY_STARTED")
                }
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> {
                    Log.e(TAG, "onScanFailed: SCAN_FAILED_APPLICATION_REGISTRATION_FAILED")
                }
                SCAN_FAILED_FEATURE_UNSUPPORTED -> {
                    Log.e(TAG, "onScanFailed: SCAN_FAILED_FEATURE_UNSUPPORTED")
                }
                SCAN_FAILED_INTERNAL_ERROR -> {
                    Log.e(TAG, "onScanFailed: SCAN_FAILED_INTERNAL_ERROR")
                }
                else -> Log.e(TAG, "onScanFailed: UN_KNOW")
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }
    }

    private val gattCallBack: BluetoothGattCallback by lazy {
        object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                Log.e(
                    TAG,
                    "onConnectionStateChange: status: $status isConnect: $newState ${gatt?.device?.address}"
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_SCAN
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionCallback.requestPermission()
                        return
                    }
                }
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            bleClientCallBack.deviceState(
                                gatt?.device?.address ?: "",
                                gatt?.device?.name,
                                StateConnect.CONNECTED
                            )
                            bleClientCallBack.onServicesDiscovered(false)
                            gatt?.discoverServices()
                        }
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        bleClientCallBack.deviceState(
                            gatt?.device?.address ?: "",
                            gatt?.device?.name,
                            StateConnect.CONNECTING
                        )
                    }
                    BluetoothProfile.STATE_DISCONNECTING -> {
                        bleClientCallBack.deviceState(
                            gatt?.device?.address ?: "",
                            gatt?.device?.name,
                            StateConnect.DISCONNECTING
                        )
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        bleClientCallBack.deviceState(
                            gatt?.device?.address ?: "",
                            gatt?.device?.name,
                            StateConnect.DISCONNECTED
                        )
                        bleClientCallBack.onServicesDiscovered(false)
                        bleClientCallBack.onBonded(false)
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                bleClientCallBack.onServicesDiscovered(status == BluetoothGatt.GATT_SUCCESS)
            }

            @Suppress("DEPRECATION")
            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                Log.e(TAG, "onCharacteristicRead: ")
                super.onCharacteristicRead(gatt, characteristic, status)
                val value = characteristic?.value ?: return
                val id = gatt?.device?.address ?: return
                bleClientCallBack.onCharacteristicRead(id, characteristic.uuid.toString(), value)
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
                Log.e(TAG, "onCharacteristicRead: ")
                super.onCharacteristicRead(gatt, characteristic, value, status)
                val id = gatt.device?.address ?: return
                bleClientCallBack.onCharacteristicRead(id, characteristic.uuid.toString(), value)
            }

            @Suppress("DEPRECATION")
            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
            ) {
                Log.e(TAG, "onCharacteristicChanged: ")
                super.onCharacteristicChanged(gatt, characteristic)
                val value = characteristic?.value ?: return
                val id = gatt?.device?.address ?: return
                bleClientCallBack.onCharacteristicChanged(id, characteristic.uuid.toString(), value)
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                Log.e(TAG, "onCharacteristicChanged: ")
                super.onCharacteristicChanged(gatt, characteristic, value)
                val id = gatt.device?.address ?: return
                bleClientCallBack.onCharacteristicChanged(id, characteristic.uuid.toString(), value)
            }
        }
    }
//#endregion

    //#region ble fun
    @kotlin.jvm.Throws(java.lang.Exception::class)
    override fun startScan(serviceUuids: List<String>) {
        if (!FunctionUtils.checkPermission(context)) {
            permissionCallback.requestPermission()
            return
        }
        if (!FunctionUtils.isLocationEnabled(context)) {
            permissionCallback.requestEnableLocation()
            return
        }
        stopScan()
        val argsFilter: List<ScanFilter> = serviceUuids.map {
            ScanFilter.Builder().setServiceUuid(
                ParcelUuid(UuidConvert.checkUUID(it))
            ).build()
        }.toList()
        val scanSettings: ScanSettings =
            ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        isScanInProgress = true
        scanner.startScan(argsFilter, scanSettings, scanCallback)
    }

    override fun stopScan() {
        if (!FunctionUtils.checkPermission(context)) {
            permissionCallback.requestPermission()
            return
        }
        isScanInProgress = false
        scanner.stopScan(scanCallback)
    }

    //#region subscriber
    override fun subscriber(
        gatt: BluetoothGatt,
        characteristicId: String,
        baseUUID: String
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setCharacteristicNotificationT(
                UuidConvert.checkUUID(baseUUID),
                UuidConvert.checkUUID(characteristicId),
                gatt
            )
        } else {
            setCharacteristicNotification(
                UuidConvert.checkUUID(baseUUID),
                UuidConvert.checkUUID(characteristicId),
                gatt
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setCharacteristicNotificationT(
        serviceUuid: UUID,
        characteristicUuid: UUID,
        gatt: BluetoothGatt
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionCallback.requestPermission()
                return false
            }
        }
        val characteristic = gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
        characteristic ?: return false
        val data = gatt.setCharacteristicNotification(characteristic, true)
        val descriptor =
            characteristic.getDescriptor(UuidConvert.checkUUID("2902"))
        return if (descriptor != null) {
            Handler(Looper.getMainLooper()).postDelayed({
            }, 1000)
            when (gatt.writeDescriptor(
                descriptor,
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            )) {
                BluetoothStatusCodes.SUCCESS -> true
                BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION -> {
                    Log.e(
                        PluginController::class.simpleName,
                        "writeCharacteristicT: ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION"
                    )
                    false
                }
                BluetoothStatusCodes.ERROR_GATT_WRITE_NOT_ALLOWED -> {
                    Log.e(
                        PluginController::class.simpleName,
                        "writeCharacteristicT: ERROR_GATT_WRITE_NOT_ALLOWED"
                    )
                    false
                }
                BluetoothStatusCodes.ERROR_UNKNOWN -> {
                    Log.e(
                        PluginController::class.simpleName,
                        "writeCharacteristicT: ERROR_UNKNOWN"
                    )
                    false
                }
                BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY -> {
                    Log.e(
                        PluginController::class.simpleName,
                        "writeCharacteristicT: ERROR_GATT_WRITE_REQUEST_BUSY"
                    )
                    false
                }
                BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND -> {
                    Log.e(
                        PluginController::class.simpleName,
                        "writeCharacteristicT: ERROR_PROFILE_SERVICE_NOT_BOUND"
                    )
                    false
                }
                else -> {
                    Log.e(PluginController::class.simpleName, "writeCharacteristicT: UnKnown")
                    false
                }
            }
        } else {
            data
        }
    }

    @Suppress("DEPRECATION")
    private fun setCharacteristicNotification(
        serviceUuid: UUID, characteristicUuid: UUID, gatt: BluetoothGatt
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionCallback.requestPermission()
                return false
            }
        }
        val characteristic = gatt.getService(serviceUuid)?.getCharacteristic(characteristicUuid)
        characteristic ?: return false
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor =
            characteristic.getDescriptor(UuidConvert.checkUUID("2902"))
        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        return Handler(Looper.getMainLooper()).postDelayed({
            gatt.writeDescriptor(descriptor)
        }, 1000)
    }
    //#endregion

    override fun connect(deviceId: String): BluetoothGatt {
        if (!FunctionUtils.checkPermission(context)) {
            permissionCallback.requestPermission()
            throw Exception()
        }
        stopScan()
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            adapter.getRemoteLeDevice(deviceId, BluetoothDevice.ADDRESS_TYPE_PUBLIC)
        } else {
            adapter.getRemoteDevice(deviceId)
        }
        val gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallBack, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallBack)
        }
        val bonded = gatt.device.bondState == BluetoothDevice.BOND_BONDED
        bleClientCallBack.onBonded(bonded)
        return gatt
    }

    private fun refreshDeviceCache(gatt: BluetoothGatt?): Boolean {
        val localGatt: BluetoothGatt = gatt ?: return false
        val localMethod = localGatt.javaClass.getMethod("refresh")
        return localMethod.invoke(localGatt) as Boolean
    }

    override fun disconnect(gatt: BluetoothGatt?) {
        if (!FunctionUtils.checkPermission(context)) {
            permissionCallback.requestPermission()
            return
        }
        Log.e(TAG, "disconnect: ${gatt?.device?.address}")
        gatt?.disconnect()
        gatt?.close()
        gatt?.let {
            refreshDeviceCache(it)
        }
    }

    fun disconnectAll(gattArgs: List<BluetoothGatt>) {
        for (gatt in gattArgs) {
            disconnect(gatt)
        }
    }

    override fun readCharacteristic(
        gatt: BluetoothGatt,
        serviceId: String,
        characteristicId: String
    ): Boolean {
        if (!FunctionUtils.checkPermission(context)) {
            throw Exception()
        }
        val service = gatt.getService(UuidConvert.checkUUID(serviceId)) ?: return false
        val characteristic =
            service.getCharacteristic(UuidConvert.checkUUID(characteristicId))
                ?: return false
        return gatt.readCharacteristic(characteristic)
    }

    //#region writeCharacteristic
    override fun writeCharacteristic(
        gatt: BluetoothGatt,
        serviceId: String,
        characteristicId: String,
        value: List<Int>,
    ): Boolean {
        if (!FunctionUtils.checkPermission(context)) {
            permissionCallback.requestPermission()
            throw Exception()
        }
        val valueArgs = value.map { it.toByte() }.toByteArray()
        val service = gatt.getService(UuidConvert.checkUUID(serviceId))
        if (service == null) {
            Log.e(TAG, "writeCharacteristic: service null")
            return false
        }
        val characteristic =
            service.getCharacteristic(UuidConvert.checkUUID(characteristicId))
        if (characteristic == null) {
            Log.e(TAG, "writeCharacteristic: char null")
            return false
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            writeCharacteristicT(characteristic, gatt, valueArgs)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            writeCharacteristicS(characteristic, gatt, valueArgs)
        } else {
            writeCharacteristicO(characteristic, gatt, valueArgs)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int) {
//        if (requestCode == FlutterFhcBlePlugin.REQUEST_PERMISSION && resultCode == PackageManager.PERMISSION_GRANTED) {
//        }
    }

    @Suppress("DEPRECATION")
    private fun writeCharacteristicO(
        characteristic: BluetoothGattCharacteristic,
        gatt: BluetoothGatt,
        value: ByteArray
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionCallback.requestPermission()
                return false
            }
        }
        characteristic.value = value
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        return try {
            gatt.writeCharacteristic(characteristic)
        } catch (e: Exception) {
            Log.e(TAG, "writeCharacteristicO: ${e.localizedMessage}")
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun writeCharacteristicS(
        characteristic: BluetoothGattCharacteristic,
        gatt: BluetoothGatt,
        value: ByteArray
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionCallback.requestPermission()
                return false
            }
        }
        characteristic.value = value
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        return try {
            gatt.writeCharacteristic(characteristic)
        } catch (e: Exception) {
            Log.e(TAG, "writeCharacteristicS: ${e.localizedMessage}")
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun writeCharacteristicT(
        characteristic: BluetoothGattCharacteristic,
        gatt: BluetoothGatt,
        value: ByteArray
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionCallback.requestPermission()
                return false
            }
        }
        val data = gatt.writeCharacteristic(
            characteristic,
            value,
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        )
        Log.e(TAG, "writeCharacteristicT: $data")
        return when (data) {
            BluetoothStatusCodes.SUCCESS -> true
            BluetoothStatusCodes.ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION -> {
                Log.e(TAG, "writeCharacteristicT: ERROR_MISSING_BLUETOOTH_CONNECT_PERMISSION")
                false
            }
            BluetoothStatusCodes.ERROR_GATT_WRITE_NOT_ALLOWED -> {
                Log.e(TAG, "writeCharacteristicT: ERROR_GATT_WRITE_NOT_ALLOWED")
                false
            }
            BluetoothStatusCodes.ERROR_UNKNOWN -> {
                Log.e(TAG, "writeCharacteristicT: ERROR_UNKNOWN")
                false
            }
            BluetoothStatusCodes.ERROR_GATT_WRITE_REQUEST_BUSY -> {
                Log.e(TAG, "writeCharacteristicT: ERROR_GATT_WRITE_REQUEST_BUSY")
                false
            }
            BluetoothStatusCodes.ERROR_PROFILE_SERVICE_NOT_BOUND -> {
                Log.e(TAG, "writeCharacteristicT: ERROR_PROFILE_SERVICE_NOT_BOUND")
                false
            }
            else -> {
                Log.e(TAG, "writeCharacteristicT: UnKnown")
                false
            }
        }
    }

    fun unBonded(deviceId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionCallback.requestPermission()
                return
            }
        }
        for (device in adapter.bondedDevices) {
            try {
                if (device.address == deviceId) {
                    val removeBond: Method = device.javaClass.getMethod("removeBond", null)
                    removeBond.invoke(device, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "unBonded: Removing has been failed: ${e.localizedMessage}")
            }
        }
    }
//#endregion
    //#endregion

    //#region broadcast
    private val receiverStatus = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            Log.e(TAG, "onReceive: ${p1?.action}")
            when (p1?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (p1.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_OFF -> bleStatus.onStatus(StatusBluetooth.OFF)
                        BluetoothAdapter.STATE_ON -> {
                            if (!this@BleClient::adapter.isInitialized) {
                                initBle()
                            }
                            bleStatus.onStatus(StatusBluetooth.ON)
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> bleStatus.onStatus(StatusBluetooth.TURING_OFF)
                        BluetoothAdapter.STATE_TURNING_ON -> bleStatus.onStatus(StatusBluetooth.TURING_ON)
                        else -> bleStatus.onStatus(StatusBluetooth.OFF)
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    when (p1.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)) {
                        BluetoothDevice.BOND_BONDING -> bleClientCallBack.onBonded(false)
                        BluetoothDevice.BOND_BONDED -> bleClientCallBack.onBonded(true)
                        BluetoothDevice.BOND_NONE -> bleClientCallBack.onBonded(false)
                        else -> bleClientCallBack.onBonded(false)
                    }
                }
            }
        }
    }

    fun registerBroadCast() {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        Log.e(TAG, "registerBroadCast: register")
        context.registerReceiver(receiverStatus, filter)
    }

    fun onDestroy() {
        context.unregisterReceiver(receiverStatus)
    }
    //#endregion
}