package com.tutorial.wifip2ptutorial

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat


public class WifiDirectBroadcastReceiver(
    val mManager: WifiP2pManager,
    val mChannel: Channel,
    private val mActivity: MainActivity
) : BroadcastReceiver () {

    private val TAG: String = "WifiBroadCastReceiver"


    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received a signal!")
        var action = intent.action

        if (WifiManager.WIFI_STATE_CHANGED_ACTION == action) {
            var wifiStatus = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
            when (wifiStatus) {
                WifiManager.WIFI_STATE_ENABLED -> {
                    Toast.makeText(context, "Wifi is now on", Toast.LENGTH_SHORT).show()
                }
                WifiManager.WIFI_STATE_DISABLED -> {
                    Toast.makeText(context, "Wifi is now off", Toast.LENGTH_SHORT).show()
                }
            }
        }
        else if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action){
            //1
        }
        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {
            if (mManager != null) {
                if (ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                mManager.requestPeers(mChannel, mActivity.peerListListener)
            }
        }
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {
            if (mManager == null) {
                return
            }
            var networkInfo: NetworkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO)!!
            if (networkInfo.isConnected) {
                mManager.requestConnectionInfo(mChannel, mActivity.connectionInfoListener)
            }
            else {
                mActivity.connectionStatus.text = "Device Disconnected"
            }
        }
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {
            // 4
        }
    }
}