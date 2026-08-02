package com.holidate.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A person we have discovered through the mesh (or our own profile, when [nodeId] equals
 * the local node id). Carries the public keys needed to verify their messages and encrypt to them.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val nodeId: String,
    val displayName: String,
    val age: Int,
    val bio: String,
    val interests: List<String>,
    /** Base64 JPEG thumbnail, kept small so it can flood the mesh cheaply. */
    val photo: String?,
    val signingKey: String,
    val encryptionKey: String,
    val lastSeen: Long,
    val isSelf: Boolean = false,
)

/** Records our swipe decision on another profile. */
@Entity(tableName = "swipes")
data class SwipeEntity(
    @PrimaryKey val nodeId: String,
    val liked: Boolean,
    val decidedAt: Long,
)

/** Someone who liked us before we had decided on them. Drives mutual-match detection. */
@Entity(tableName = "incoming_likes")
data class IncomingLikeEntity(
    @PrimaryKey val nodeId: String,
    val receivedAt: Long,
)

/** A mutual like. Chat unlocks only once a row exists here. */
@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val nodeId: String,
    val matchedAt: Long,
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val peerId: String,
    val text: String,
    val outgoing: Boolean,
    val timestamp: Long,
    val delivered: Boolean = false,
)
