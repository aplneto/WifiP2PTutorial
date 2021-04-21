package com.tutorial.wifip2ptutorial

import android.os.Handler
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

public class ClientClass (private var mainActivity: MainActivity): Thread () {
    lateinit var socket: Socket
    lateinit var hostAdd: String
    lateinit var sendReceiveClass: SendReceiveClass

    constructor(hostAddress: InetAddress, mainActivity: MainActivity) :  this(mainActivity) {
        this.hostAdd = hostAddress.hostAddress
        this.socket = Socket()
    }

    override fun run() {
        try {
            socket.connect(InetSocketAddress(hostAdd, 10001))
            sendReceiveClass = SendReceiveClass(socket, mainActivity)
            sendReceiveClass.start()
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }

}