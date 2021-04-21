package com.tutorial.wifip2ptutorial

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.*
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.InetAddress

class MainActivity : AppCompatActivity() {

    private lateinit var buttonOnOff: Button
    private lateinit var buttonDiscover: Button
    private lateinit var buttonSend: Button
    private lateinit var writeMsg: EditText
    lateinit var connectionStatus: TextView
    lateinit var readMsg: TextView
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

    lateinit var connectionInfoListener: WifiP2pManager.ConnectionInfoListener

    lateinit var handler: Handler

    val MESSAGE_READ: Int = 1

    lateinit var serverClass: ServerClass
    lateinit var clientClass: ClientClass



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
        readMsg = findViewById(R.id.readMsg)
        buttonSend = findViewById(R.id.sendButton)
        writeMsg = findViewById(R.id.writeMsg)

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

        connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info ->
            val groupOwner: InetAddress = info.groupOwnerAddress

            if (info.groupFormed && info.isGroupOwner) {
                connectionStatus.text = "Host"
                serverClass = ServerClass(this)
                serverClass.start()
            } else if (info.groupFormed) {
                connectionStatus.text = "Client"
                clientClass = ClientClass(groupOwner, this)
                clientClass.start()
            }
        }

        handler = Handler(object : Handler.Callback {
            override fun handleMessage(msg: Message): Boolean {
                when (msg.what)
                {
                    MESSAGE_READ -> {
                        var readBuff : ByteArray = msg.obj as ByteArray
                        var tempMsg: String = String(readBuff,0, msg.arg1)
                        readMsg.text = tempMsg
                    }
                }
                return true;
            }
        })

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

        listView.setOnItemClickListener(
            object : AdapterView.OnItemClickListener {
                override fun onItemClick(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    val device: WifiP2pDevice = deviceArray[position]!!
                    var config: WifiP2pConfig = WifiP2pConfig()
                    config.deviceAddress = device.deviceAddress

                    if (ActivityCompat.checkSelfPermission(applicationContext,
                            Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    mManager.connect(
                        mChannel, config, object : WifiP2pManager.ActionListener {
                            override fun onSuccess() {
                                Toast.makeText(applicationContext, "Connected to ${device.deviceName}", Toast.LENGTH_SHORT).show()
                            }

                            override fun onFailure(reason: Int) {
                                Toast.makeText(applicationContext, "Not connected to ${device.deviceName}", Toast.LENGTH_SHORT).show()
                            }

                        }
                    )
                }

            }
        )

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

        buttonSend.setOnClickListener {
            var msg: ByteArray = writeMsg.text.toString().toByteArray()
            if (this::serverClass.isInitialized) {
                serverClass.sendReceiveClass.write(msg)
            }
            else {
                clientClass.sendReceiveClass.write(msg)
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

    override fun onResume() {
        super.onResume()
        registerReceiver(mReceiver, mIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mReceiver)
    }
}