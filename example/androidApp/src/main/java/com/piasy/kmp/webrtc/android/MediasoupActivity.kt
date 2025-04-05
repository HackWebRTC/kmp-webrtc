package com.piasy.kmp.webrtc.android

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import org.mediasoup.droid.lib.Protoo
import org.mediasoup.droid.lib.Utils
import org.mediasoup.droid.lib.socket.WebSocketTransport
import org.protoojs.droid.Message
import org.protoojs.droid.Peer

class MediasoupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mediasoup)

//        Logger.setLogLevel(Logger.LogLevel.LOG_TRACE)
//        Logger.setDefaultHandler()
//        MediasoupClient.initialize(applicationContext)

        Thread(this::test).start()
//        test2()
    }

    private lateinit var handler: Handler
    private lateinit var protoo: Protoo
    private fun test() {
        val thread = HandlerThread("worker")
        thread.start()
        handler = Handler(thread.looper)

        handler.post {
            val transport = WebSocketTransport("wss://v3demo.mediasoup.org:4443/?roomId=piasytest&peerId=${Utils.getRandomString(8)}")
            protoo = Protoo(transport, object : Peer.Listener {
                override fun onOpen() {
                    handler.post {
                        val routerRtpCapabilities = protoo.syncRequest("getRouterRtpCapabilities")

                        Log.d("XXPXX", "routerRtpCapabilities $routerRtpCapabilities")
                    }
                }

                override fun onFail() {
                }

                override fun onRequest(
                    request: Message.Request,
                    handler: Peer.ServerRequestHandler
                ) {
                }

                override fun onNotification(notification: Message.Notification) {
                }

                override fun onDisconnected() {
                }

                override fun onClose() {
                }
            })
        }
    }
}