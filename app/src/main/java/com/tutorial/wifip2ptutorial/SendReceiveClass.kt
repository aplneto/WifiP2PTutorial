package com.tutorial.wifip2ptutorial

import android.content.Context
import android.os.Handler
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class SendReceiveClass (
    private var socket: Socket,
    private var mainActivity: MainActivity
    )
    : Thread() {
    private var handler: Handler = mainActivity.handler
    private var inputStream: InputStream = socket.getInputStream()
    private var outputStream: OutputStream = socket.getOutputStream()


    override fun run() {
        var buffer: ByteArray = ByteArray(1024)
        var bytes: Int
        while (socket != null ) {
            try {
                bytes = inputStream.read(buffer)
                if (bytes > 0) {
                    handler.obtainMessage(mainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget()
                }
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun write(bytes: ByteArray) {
        val thread = Thread {
            try {
                outputStream.write(bytes)
            }
            catch (e: IOException) {
                e.printStackTrace()
            }
        }
        thread.start()
    }
}