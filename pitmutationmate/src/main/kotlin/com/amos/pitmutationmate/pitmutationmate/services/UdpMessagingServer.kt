// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2023 Lennart Heimbs

package com.amos.pitmutationmate.pitmutationmate.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.ToolWindowManager
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException

/**
 * A simple UDP server that listens on a given port and prints the received messages.
 */
@Service(Service.Level.PROJECT)
class UdpMessagingServer(private val project: Project) {

    private val log: Logger = Logger.getInstance(UdpMessagingServer::class.java)
    private var isRunning = false
    private var socket: DatagramSocket? = null
    private var receiveThread: Thread? = null
    private val _port: Int = findAvailablePort()

    companion object {
        private const val MIN_PORT_NUMBER = 49152
        private const val MAX_PORT_NUMBER = 65535
        private const val RECEIVE_BUFFER_SIZE = 256
        private const val MESSAGE_LENGTH_OFFSET = 0
        private const val MESSAGE_START_OFFSET = 1
    }

    val port: Int
        get() = _port

    /**
     * Starts the UDP server if it is not already running.
     * @param overrideClassFQN If provided, will notify the user when a message containing this FQN is received.
     */
    fun startServer(overrideClassFQN: String? = null) {
        if (!isRunning) {
            try {
                socket = DatagramSocket(_port)
                isRunning = true
                receiveThread = Thread { receiveMessages(overrideClassFQN) }
                receiveThread?.start()
                log.info("UdpMessagingServer started on port $_port")
            } catch (e: SocketException) {
                log.error("Failed to start UDP server on port $_port", e)
            }
        }
    }

    /**
     * Stops the UDP server if it is running.
     */
    fun stopServer() {
        isRunning = false
        socket?.close()
        receiveThread?.interrupt()
        log.info("UdpMessagingServer stopped")
    }

    /**
     * Receives messages in a loop while the server is running.
     * Notifies the user if a message containing [overrideClassFQN] is received.
     */
    private fun receiveMessages(overrideClassFQN: String? = null) {
        val localSocket = socket ?: return
        while (isRunning) {
            val receiveData = ByteArray(RECEIVE_BUFFER_SIZE)
            val receivePacket = DatagramPacket(receiveData, receiveData.size)
            try {
                localSocket.receive(receivePacket)
                val data = receivePacket.data
                val messageLength = data[MESSAGE_LENGTH_OFFSET].toInt() and 0xFF // Read the length byte
                val message = String(data, MESSAGE_START_OFFSET, messageLength) // Extract the message
                log.trace("Received message: $message")

                if (overrideClassFQN != null && message.contains(overrideClassFQN)) {
                    ToolWindowManager.getInstance(project).notifyByBalloon(
                        "Pitest",
                        MessageType.INFO,
                        "<p>Successfully applied pitest target class</p><p>$overrideClassFQN.</p>"
                    )
                }
            } catch (e: IOException) {
                if (isRunning) {
                    log.warn("Error receiving UDP packet", e)
                }
                break
            } catch (e: Exception) {
                log.error("Unexpected error in UDP server", e)
                break
            }
        }
    }

    /**
     * Finds an available UDP port in the dynamic/private range.
     * @return An available port number.
     * @throws IllegalStateException if no available port is found.
     */
    private fun findAvailablePort(): Int {
        for (port in MIN_PORT_NUMBER..MAX_PORT_NUMBER) {
            try {
                DatagramSocket(port).use {
                    // If we can bind, the port is available
                    return port
                }
            } catch (e: SocketException) {
                // Port is not available, continue searching
            }
        }
        error("No available port found in the dynamic range")
    }
}
