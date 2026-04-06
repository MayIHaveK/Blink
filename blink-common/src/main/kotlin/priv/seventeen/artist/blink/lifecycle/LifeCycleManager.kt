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
package priv.seventeen.artist.blink.lifecycle

import priv.seventeen.artist.blink.BlinkLog
import java.util.*

object LifeCycleManager {

    private data class PriorityHandler(
        val priority: Int,
        val action: Runnable,
        val name: String
    )

    private val handlers = EnumMap<LifeCycle, MutableList<PriorityHandler>>(LifeCycle::class.java)
    private val sortedCache = EnumMap<LifeCycle, List<PriorityHandler>>(LifeCycle::class.java)

    fun register(lifeCycle: LifeCycle, priority: Int, action: Runnable, name: String = "") {
        handlers.getOrPut(lifeCycle) { mutableListOf() }
            .add(PriorityHandler(priority, action, name))
        sortedCache.remove(lifeCycle)
    }

    fun trigger(lifeCycle: LifeCycle) {
        val list = sortedCache.getOrPut(lifeCycle) {
            handlers[lifeCycle]?.sortedBy { it.priority } ?: emptyList()
        }
        for (handler in list) {
            try {
                handler.action.run()
            } catch (e: Throwable) {
                BlinkLog.error("@Awake(${lifeCycle.name}) ${handler.name} 执行出错", e)
            }
        }
    }

    fun clear() {
        handlers.clear()
        sortedCache.clear()
    }
}
