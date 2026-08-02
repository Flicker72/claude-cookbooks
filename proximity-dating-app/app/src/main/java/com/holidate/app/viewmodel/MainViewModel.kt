package com.holidate.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.holidate.app.HoliDateApp
import com.holidate.app.data.db.ProfileEntity
import com.holidate.app.mesh.MeshController
import com.holidate.app.mesh.MeshForegroundService
import com.holidate.app.data.repository.HoliDateRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives onboarding, discovery, matches and the mesh toggle. Chat has its own
 * [ChatViewModel] because it is scoped to a single peer.
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as HoliDateApp).container
    private val repository: HoliDateRepository = container.repository
    private val mesh: MeshController = container.meshController

    val selfProfile: StateFlow<ProfileEntity?> =
        repository.selfProfile.stateInEagerly(null)

    val discoverable: StateFlow<List<ProfileEntity>> =
        repository.discoverable.stateInEagerly(emptyList())

    val matches: StateFlow<List<ProfileEntity>> =
        repository.matches.stateInEagerly(emptyList())

    val meshRunning: StateFlow<Boolean> = mesh.running
    val nearbyPeerCount: StateFlow<Int> = mesh.nearbyPeerCount

    fun saveProfile(name: String, age: Int, bio: String, interests: List<String>, photo: String?) {
        viewModelScope.launch {
            repository.saveOwnProfile(name, age, bio, interests, photo)
        }
    }

    fun swipe(nodeId: String, liked: Boolean) {
        viewModelScope.launch { repository.swipe(nodeId, liked) }
    }

    /** Start or stop the background mesh service. */
    fun setMeshEnabled(enabled: Boolean) {
        val context = getApplication<Application>()
        if (enabled) MeshForegroundService.start(context) else MeshForegroundService.stop(context)
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.stateInEagerly(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.Eagerly, initial)
}
