package com.holidate.app

import android.content.Context
import com.holidate.app.crypto.KeyManager
import com.holidate.app.data.db.HoliDateDatabase
import com.holidate.app.data.repository.HoliDateRepository
import com.holidate.app.mesh.MeshController
import com.holidate.app.mesh.MessageRouter
import com.holidate.app.mesh.NearbyMeshTransport

/**
 * Manual dependency container. Constructs the object graph once and resolves the
 * repository ⇄ router cycle explicitly rather than pulling in a DI framework for an MVP.
 */
class AppContainer(context: Context) {

    val keyManager: KeyManager = KeyManager.getOrCreate(context)

    private val database: HoliDateDatabase = HoliDateDatabase.get(context)

    val repository: HoliDateRepository = HoliDateRepository(database, keyManager)

    private val transport = NearbyMeshTransport(context)

    private val router: MessageRouter = MessageRouter(keyManager, transport, repository)

    val meshController: MeshController = MeshController(transport, repository)

    init {
        // Close the cycle: the repository needs the router to originate messages.
        repository.router = router
    }
}
