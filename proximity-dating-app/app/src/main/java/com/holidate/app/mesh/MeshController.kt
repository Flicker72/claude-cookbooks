package com.holidate.app.mesh

import com.holidate.app.data.repository.HoliDateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the on/off lifecycle of the mesh: starts the radio transport, keeps re-broadcasting our
 * profile beacon on an interval, and exposes observable running/peer-count state to the UI.
 */
class MeshController(
    private val transport: MeshTransport,
    private val repository: HoliDateRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var beaconJob: Job? = null

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    /** Number of phones we are directly connected to right now (one hop away). */
    val nearbyPeerCount: StateFlow<Int> = transport.connectedPeersCount(scope)

    fun start() {
        if (_running.value) return
        // A short, non-identifying label. Identity lives in the signed beacon, not here.
        transport.start(localName = "holidate")
        _running.value = true
        beaconJob = scope.launch {
            while (isActive) {
                repository.broadcastOwnBeacon()
                delay(HoliDateRepository.BEACON_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        beaconJob?.cancel()
        beaconJob = null
        transport.stop()
        _running.value = false
    }
}

/** Maps the transport's peer-id set into a live count for the UI. */
private fun MeshTransport.connectedPeersCount(scope: CoroutineScope): StateFlow<Int> {
    val out = MutableStateFlow(0)
    scope.launch {
        connectedPeers.collect { out.value = it.size }
    }
    return out.asStateFlow()
}
