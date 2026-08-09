package com.holidate.app.mesh

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstracts the radio layer that carries mesh bytes between neighbouring phones.
 *
 * The default implementation is [NearbyMeshTransport] (Google Nearby Connections over
 * BLE / Bluetooth / Wi-Fi). Keeping this as an interface means a pure-BLE or Wi-Fi Aware
 * transport can be swapped in later without touching the router or the app.
 */
interface MeshTransport {
    /** Endpoint ids of the phones we are currently directly connected to (one hop away). */
    val connectedPeers: StateFlow<Set<String>>

    fun setListener(listener: Listener)

    /** Begin advertising and discovering. [localName] is a short, non-identifying label. */
    fun start(localName: String)

    fun stop()

    /** Send raw bytes to every directly connected peer. */
    fun broadcast(bytes: ByteArray)

    /** Send raw bytes to one directly connected peer. */
    fun sendTo(endpointId: String, bytes: ByteArray)

    interface Listener {
        fun onPayloadReceived(fromEndpointId: String, bytes: ByteArray)
        fun onPeerConnected(endpointId: String)
        fun onPeerDisconnected(endpointId: String)
    }
}
