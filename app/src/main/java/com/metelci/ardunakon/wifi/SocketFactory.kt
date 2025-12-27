package com.metelci.ardunakon.wifi

import java.net.DatagramSocket
import java.net.InetSocketAddress

interface SocketFactory {
    fun createDiscoverySocket(): DatagramSocket
    fun createConnectionSocket(): DatagramSocket
}

class DefaultSocketFactory : SocketFactory {
    override fun createDiscoverySocket(): DatagramSocket {
        return try {
            DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                soTimeout = 4000
                bind(InetSocketAddress(8888))
            }
        } catch (e: Exception) {
            DatagramSocket().apply {
                broadcast = true
                soTimeout = 4000
            }
        }
    }

    override fun createConnectionSocket(): DatagramSocket {
        return DatagramSocket()
    }
}
