package com.tutorial.wifip2ptutorial

import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var buttonOnOff: Button
    private lateinit var buttonDiscover: Button
    private lateinit var connectionStatus: TextView
    private lateinit var listView: ListView

    private lateinit var wifiManager: WifiManager

    private lateinit var mManager: WifiP2pManager
    private lateinit var mChannel: Channel
    private lateinit var mReceiver: WifiDirectBroadcastReceiver
    private lateinit var mIntentFilter : IntentFilter

    private val FINE_LOCATION_RQ = 101

    lateinit var peerListListener: WifiP2pManager.PeerListListener

    private var peers = mutableListOf<WifiP2pDevice>()
    private lateinit var deviceNameArray: Array<String>
    private lateinit var deviceArray: Array<WifiP2pDevice?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startComponents()
        configButtons()

    }

    private fun startComponents() {

        buttonOnOff = findViewById(R.id.onOff)
        buttonDiscover = findViewById(R.id.discover)
        connectionStatus = findViewById(R.id.connectionStatus)
        listView = findViewById(R.id.peerListView)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        mManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        mChannel = mManager.initialize(this, Looper.getMainLooper(), null)
        mReceiver = WifiDirectBroadcastReceiver(mManager, mChannel, this)

        mIntentFilter = IntentFilter()

        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)

        registerReceiver(mReceiver, mIntentFilter)

        peerListListener = object: WifiP2pManager.PeerListListener {
            override fun onPeersAvailable(peerList: WifiP2pDeviceList) {
                if (peerList.deviceList != peers) {
                    peers.clear()
                    peers.addAll(peerList.deviceList)

                    deviceNameArray = Array(size = peerList.deviceList.size) { "" }
                    deviceArray = arrayOfNulls(peerList.deviceList.size)

                    for ((index, device: WifiP2pDevice) in peerList.deviceList.withIndex()) {
                        deviceNameArray[index] = device.deviceName
                        deviceArray[index] = device
                    }

                    var adapter: ArrayAdapter<String> = ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, deviceNameArray)
                    listView.adapter = adapter
                }

                if (peers.size == 0) {
                    Toast.makeText(applicationContext, "No devices found", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }

    }

    private fun configButtons() {
        buttonOnOff.setOnClickListener {
            if (wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false)
                buttonOnOff.text = "Wifi Off"
            }
            else {
                wifiManager.setWifiEnabled(true)
                buttonOnOff.text = "Wifi On"
            }
        }
        if (!wifiManager.isWifiEnabled()) buttonOnOff.text = "Wifi Off"

        buttonDiscover.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                var permission: String = android.Manifest.permission.ACCESS_FINE_LOCATION
                when {
                    ContextCompat.checkSelfPermission(applicationContext, permission) == PackageManager.PERMISSION_GRANTED -> {
                        mManager.discoverPeers(
                            mChannel,
                            object: WifiP2pManager.ActionListener {
                                override fun onSuccess() {
                                    connectionStatus.text = "Discovery started"
                                }
                                override fun onFailure(reasonCode: Int) {
                                    connectionStatus.text = "Discovery starting failed"
                                    Log.d("Main", "Error code: $reasonCode")
                                }
                            }
                        )
                    }
                    shouldShowRequestPermissionRationale(permission) -> presentDialog(
                        permission, "Location", FINE_LOCATION_RQ)
                    else -> ActivityCompat.requestPermissions(this, arrayOf(permission), FINE_LOCATION_RQ)
                }
            }
        }
    }


    private fun presentDialog(permission: String, name: String, requestCode: Int) {
        val dBuilder = AlertDialog.Builder(this)

        dBuilder.apply {
            setTitle("Permission required")
            setMessage("Permission to access your $name is required to use this app")
            setPositiveButton("OK") { dialog, which ->
                ActivityCompat.requestPermissions( this@MainActivity, arrayOf(permission), requestCode)
            }
        }
        val dialog = dBuilder.create()
        dialog.show()
    }

    override fun onDestroy() {
        unregisterReceiver(mReceiver)
        super.onDestroy()
    }
}