package com.example.testplugin.service

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CooldownManager {

    private val cooldowns = ConcurrentHashMap<String, Long>()

    private fun key(uuid: UUID, action: String) = "$uuid:$action"

    fun setCooldown(uuid: UUID, action: String, durationMs: Long) {
        cooldowns[key(uuid, action)] = System.currentTimeMillis() + durationMs
    }

    fun isOnCooldown(uuid: UUID, action: String): Boolean {
        val expiry = cooldowns[key(uuid, action)] ?: return false
        if (System.currentTimeMillis() >= expiry) {
            cooldowns.remove(key(uuid, action))
            return false
        }
        return true
    }

    fun remaining(uuid: UUID, action: String): Long {
        val expiry = cooldowns[key(uuid, action)] ?: return 0
        return (expiry - System.currentTimeMillis()).coerceAtLeast(0)
    }

    fun clearAll(uuid: UUID) {
        val prefix = "$uuid:"
        cooldowns.keys.removeIf { it.startsWith(prefix) }
    }
}
