package com.tutorial.wifip2ptutorial

import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

class ServerClass (private val mainActivity: MainActivity) : Thread() {
    var serverSocket: ServerSocket = ServerSocket(10001)
    lateinit var socket: Socket
    lateinit var sendReceiveClass: SendReceiveClass

    override fun run() {
        try {
            socket = serverSocket.accept()
            sendReceiveClass = SendReceiveClass(socket, mainActivity)
            sendReceiveClass.start()
        }
        catch (e: IOException) {
            e.printStackTrace()
        }
    }

}