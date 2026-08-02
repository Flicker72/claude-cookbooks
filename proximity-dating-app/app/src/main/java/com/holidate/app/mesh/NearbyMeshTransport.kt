package com.holidate.app.mesh

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [MeshTransport] backed by Google Nearby Connections using the `P2P_CLUSTER` strategy.
 *
 * `P2P_CLUSTER` is an M-to-N topology: a phone advertises and discovers at the same time and
 * can hold connections to many neighbours at once. That directly connected cluster is one hop;
 * multi-hop reach ("further afield") is layered on top by [MessageRouter], which re-broadcasts
 * envelopes so they ripple from cluster to cluster across every phone running the app.
 */
class NearbyMeshTransport(context: Context) : MeshTransport {

    private val appContext = context.applicationContext
    private val connections: ConnectionsClient = Nearby.getConnectionsClient(appContext)

    private var listener: MeshTransport.Listener? = null
    private var localName: String = "holidate"

    private val _connectedPeers = MutableStateFlow<Set<String>>(emptySet())
    override val connectedPeers: StateFlow<Set<String>> = _connectedPeers.asStateFlow()

    override fun setListener(listener: MeshTransport.Listener) {
        this.listener = listener
    }

    override fun start(localName: String) {
        this.localName = localName
        startAdvertising()
        startDiscovery()
    }

    override fun stop() {
        connections.stopAllEndpoints()
        connections.stopAdvertising()
        connections.stopDiscovery()
        _connectedPeers.value = emptySet()
    }

    override fun broadcast(bytes: ByteArray) {
        val peers = _connectedPeers.value.toList()
        if (peers.isNotEmpty()) {
            connections.sendPayload(peers, Payload.fromBytes(bytes))
        }
    }

    override fun sendTo(endpointId: String, bytes: ByteArray) {
        if (endpointId in _connectedPeers.value) {
            connections.sendPayload(endpointId, Payload.fromBytes(bytes))
        }
    }

    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connections.startAdvertising(localName, SERVICE_ID, connectionLifecycle, options)
            .addOnFailureListener { Log.w(TAG, "Advertising failed", it) }
    }

    private fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connections.startDiscovery(SERVICE_ID, endpointDiscovery, options)
            .addOnFailureListener { Log.w(TAG, "Discovery failed", it) }
    }

    private val endpointDiscovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            // Both phones advertise and discover, so both may request; Nearby resolves the
            // duplicate into a single connection, so we can simply request on discovery.
            connections.requestConnection(localName, endpointId, connectionLifecycle)
                .addOnFailureListener { Log.d(TAG, "requestConnection to $endpointId failed", it) }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
        }
    }

    private val connectionLifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Open network: auto-accept. Authenticity is enforced above the transport by
            // per-message signatures, and confidentiality by end-to-end encryption, so we do
            // not rely on Nearby's connection-level auth token here.
            connections.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    _connectedPeers.value = _connectedPeers.value + endpointId
                    listener?.onPeerConnected(endpointId)
                }
                else -> Log.d(TAG, "Connection to $endpointId not established: ${result.status}")
            }
        }

        override fun onDisconnected(endpointId: String) {
            _connectedPeers.value = _connectedPeers.value - endpointId
            listener?.onPeerDisconnected(endpointId)
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { listener?.onPayloadReceived(endpointId, it) }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    companion object {
        private const val TAG = "NearbyMeshTransport"

        /** Namespaces the mesh so only HoliDate phones discover each other. */
        private const val SERVICE_ID = "com.holidate.app.mesh"
        private val STRATEGY = Strategy.P2P_CLUSTER
    }
}
