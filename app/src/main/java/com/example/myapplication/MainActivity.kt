package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.sample_bionin.BioDevice


class MainActivity : AppCompatActivity(), PermissionCallback, IBleClientCallBack, BleStatus {

    private lateinit var binding: ActivityMainBinding
    private lateinit var btnStartScan: Button
    private lateinit var btnStopScan: Button
    private lateinit var lstDevices: ListView
    private lateinit var ipServices: EditText
    private lateinit var txtState: TextView
    private lateinit var txtStatus: TextView
    private lateinit var txtDiscovered: TextView
    private lateinit var txtBonded: TextView

    private lateinit var adapter: ArrayAdapter<*>
    private lateinit var plugin: BleClient

    private var devices: List<DeviceInfo> = listOf()

    // example bio
    private val bioDevice = BioDevice()

    /*
    * -1 none
    * 0 turnOn
    * 1 turnOff
    * 2 getModel
    **/
    private var value: Int = -1
    private fun handleValue(data: ByteArray) {
        Log.e(TAG, "handleValue: $value")
        when (value) {
            0 -> {
                Log.e(TAG, "handleValue: start get model")
                val gatt = devices.firstNotNullOf { it.gatt }
                value = 2
                Log.e(TAG, "handleValue: ${bioDevice.getModel().map { it.toInt() }}")
                //TODO: delay
                plugin.writeCharacteristic(
                    devices.firstNotNullOf { it.gatt },
                    BioDevice.baseUUID,
                    BioDevice.writeCharacteristic,
                    bioDevice.getModel().map { it.toInt() }
                )
                plugin.subscriber(
                    gatt,
                    BioDevice.notificationCharacteristic,
                    BioDevice.baseUUID
                )
            }
            2 -> {
                Log.e(TAG, "handleValue: ${bioDevice.decodeModelName(data)}")
            }
            else -> Log.e(TAG, "handleValue: $value: $data")
        }
    }

    @Suppress("PrivatePropertyName")
    private val TAG: String = "MainActivity"

    private fun updateView() {
        runOnUiThread {
            adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                devices.map { "${it.name}(${it.id})" })
            lstDevices.adapter = adapter
        }
    }

    override fun onDestroy() {
        plugin.onDestroy()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fab.setOnClickListener {
            Log.e(TAG, "onCreate: hehe")
        }
        txtState = findViewById(R.id.txtState)
        txtStatus = findViewById(R.id.txtStatus)
        txtDiscovered = findViewById(R.id.txtDiscovered)
        txtBonded = findViewById(R.id.txtBonded)

        ipServices = findViewById(R.id.ipServices)
        btnStartScan = findViewById(R.id.btnScan)
        btnStopScan = findViewById(R.id.btnStopScan)
        lstDevices = findViewById(R.id.lstDevices)

        lstDevices.setOnItemClickListener { _, _, position, id ->
            Log.e(TAG, "setOnClickListener: $id ${devices[position].id}")
            val positiveText = if (devices[position].gatt == null) "Connect" else "Disconnect"
            val dialog = AlertDialog.Builder(this)
                .setTitle("Device ${devices[position].name}")
                .setMessage("mac-address: ${devices[position].id}")
                .setCancelable(false)
                .setPositiveButton(positiveText) { dialog, _ ->
                    positiveOnclick(devices[position])
                    dialog.cancel()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .create()
            dialog.show()
        }

        btnStartScan.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                        99
                    )
                    return@setOnClickListener
                }
            }
            plugin.startScan(listOf(ipServices.text.toString()))
        }

        btnStopScan.setOnClickListener {
            plugin.stopScan()
        }

        plugin = BleClient(this, this, this, this)
        plugin.registerBroadCast()
    }

    private fun positiveOnclick(_device: DeviceInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                    99
                )
                return
            }
        }
        if (_device.gatt == null) {
            for (device in devices) {
                if (device.id == _device.id) {
                    device.gatt?.close()
                    device.gatt = null
                }
            }
            val gatt = plugin.connect(_device.id)
            for (device in devices) {
                if (device.id == _device.id) {
                    device.gatt = gatt
                }
            }
        } else {
            plugin.disconnectAll(devices.filter { it.id == _device.id }.mapNotNull { it.gatt })
        }
        updateView()
    }

    override fun requestPermission() {
        FunctionUtils.requestPermission(this)
    }


    override fun requestEnableLocation() {
        val intent = Intent(
            Settings.ACTION_LOCATION_SOURCE_SETTINGS
        )
        this.startActivity(intent)
    }

    override fun enableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        ActivityCompat.startActivityForResult(this, enableBtIntent, 1, Bundle())
    }

    override fun onScanResult(result: ScanResult) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                    99
                )
                return
            }
        }
        val info = DeviceInfo(result.device.name ?: "", result.device.address)
        if (devices.firstOrNull { it.id == info.id } == null) {
            devices = devices + info
        }
        updateView()
    }

    @SuppressLint("SetTextI18n")
    override fun deviceState(id: String, name: String?, state: StateConnect) {
        runOnUiThread {
            txtState.text = "$id (${state.name})"
        }
        when (state) {
            StateConnect.CONNECTING -> {}
            StateConnect.CONNECTED -> {
                updateView()
            }
            StateConnect.DISCONNECTING -> {}
            StateConnect.DISCONNECTED -> {
                val devicesDel = devices.filter { it.id == id }
                val gattArgs = devices.filter { it.id == id }.mapNotNull { it.gatt }
                plugin.disconnectAll(gattArgs)
                devices = devices - devicesDel.toSet()
                updateView()
            }
        }
    }

    override fun onCharacteristicRead(id: String, characteristicId: String, value: ByteArray) {
        handleValue(value)
    }

    override fun onCharacteristicChanged(id: String, characteristicId: String, value: ByteArray) {
        handleValue(value)
    }

    @SuppressLint("SetTextI18n")
    override fun onBonded(bonded: Boolean) {
        runOnUiThread {
            txtBonded.text = "Bonded: $bonded"
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onServicesDiscovered(status: Boolean) {
        runOnUiThread {
            txtDiscovered.text = "Discovered: $status"
        }
        if (status) {
            val gatt = devices.firstNotNullOf { it.gatt }
            value = 0
            val check = plugin.writeCharacteristic(
                gatt,
                BioDevice.baseUUID,
                BioDevice.writeCharacteristic,
                bioDevice.turnOn().map { it.toInt() }
            )
            Handler(Looper.getMainLooper()).postDelayed({
                val checkNotify =
                    plugin.subscriber(
                        gatt,
                        BioDevice.notificationCharacteristic,
                        BioDevice.baseUUID
                    )
                Log.e(TAG, "onServicesDiscovered: $check $checkNotify")
            }, 1000)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onStatus(status: StatusBluetooth) {
        runOnUiThread {
            txtStatus.text = "Status: $status"
        }
    }
}