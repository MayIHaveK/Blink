/*
 * Copyright 2026 17Artist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package priv.seventeen.artist.blink.event

import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import priv.seventeen.artist.blink.bukkitPlugin
import java.util.concurrent.ConcurrentHashMap

object EventManager {

    private val staticListener = object : Listener {}
    private val dynamicListeners = ConcurrentHashMap<String, Listener>()

    fun register(
        eventClass: Class<out Event>,
        priority: EventPriority,
        ignoreCancelled: Boolean,
        executor: EventExecutor
    ) {
        Bukkit.getPluginManager().registerEvent(
            eventClass, staticListener, priority, executor, bukkitPlugin, ignoreCancelled
        )
    }


    inline fun <reified T : Event> listen(
        key: String,
        priority: EventPriority = EventPriority.NORMAL,
        ignoreCancelled: Boolean = false,
        crossinline handler: (T) -> Unit
    ) {
        listen(T::class.java, key, priority, ignoreCancelled) { handler(it) }
    }

    fun <T : Event> listen(
        eventClass: Class<T>,
        key: String,
        priority: EventPriority = EventPriority.NORMAL,
        ignoreCancelled: Boolean = false,
        handler: (T) -> Unit
    ) {
        unlisten(key)

        val listener = object : Listener {}
        dynamicListeners[key] = listener

        Bukkit.getPluginManager().registerEvent(
            eventClass,
            listener,
            priority,
            { _, event ->
                if (eventClass.isInstance(event)) {
                    @Suppress("UNCHECKED_CAST")
                    handler(event as T)
                }
            },
            bukkitPlugin,
            ignoreCancelled
        )
    }

    fun unlisten(key: String): Boolean {
        val listener = dynamicListeners.remove(key) ?: return false
        HandlerList.unregisterAll(listener)
        return true
    }

    fun unlistenAll() {
        for ((_, listener) in dynamicListeners) {
            HandlerList.unregisterAll(listener)
        }
        dynamicListeners.clear()
    }

    fun isListening(key: String): Boolean = dynamicListeners.containsKey(key)

    fun keys(): Set<String> = dynamicListeners.keys.toSet()

    fun unregisterAll() {
        HandlerList.unregisterAll(staticListener)
        unlistenAll()
    }
}
