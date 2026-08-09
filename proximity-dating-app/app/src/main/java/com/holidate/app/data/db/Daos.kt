package com.holidate.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE nodeId = :nodeId LIMIT 1")
    suspend fun get(nodeId: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE isSelf = 1 LIMIT 1")
    fun observeSelf(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE isSelf = 1 LIMIT 1")
    suspend fun getSelf(): ProfileEntity?

    /**
     * Discoverable profiles: everyone but us that we have not already swiped on,
     * freshest first.
     */
    @Query(
        """
        SELECT * FROM profiles
        WHERE isSelf = 0
          AND nodeId NOT IN (SELECT nodeId FROM swipes)
        ORDER BY lastSeen DESC
        """,
    )
    fun observeDiscoverable(): Flow<List<ProfileEntity>>
}

@Dao
interface SwipeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(swipe: SwipeEntity)

    @Query("SELECT * FROM swipes WHERE nodeId = :nodeId LIMIT 1")
    suspend fun get(nodeId: String): SwipeEntity?

    @Query("SELECT liked FROM swipes WHERE nodeId = :nodeId LIMIT 1")
    suspend fun likedState(nodeId: String): Boolean?
}

@Dao
interface IncomingLikeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(like: IncomingLikeEntity)

    @Query("SELECT nodeId FROM incoming_likes WHERE nodeId = :nodeId LIMIT 1")
    suspend fun exists(nodeId: String): String?

    @Query("DELETE FROM incoming_likes WHERE nodeId = :nodeId")
    suspend fun delete(nodeId: String)
}

@Dao
interface MatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(match: MatchEntity)

    @Query("SELECT * FROM matches WHERE nodeId = :nodeId LIMIT 1")
    suspend fun get(nodeId: String): MatchEntity?

    /** Matches joined with the matched person's profile, most recent first. */
    @Query(
        """
        SELECT p.* FROM profiles p
        INNER JOIN matches m ON m.nodeId = p.nodeId
        ORDER BY m.matchedAt DESC
        """,
    )
    fun observeMatchedProfiles(): Flow<List<ProfileEntity>>
}

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: ChatMessageEntity)

    @Query("SELECT * FROM chat_messages WHERE peerId = :peerId ORDER BY timestamp ASC")
    fun observeConversation(peerId: String): Flow<List<ChatMessageEntity>>

    @Query("UPDATE chat_messages SET delivered = 1 WHERE id = :id")
    suspend fun markDelivered(id: String)
}
