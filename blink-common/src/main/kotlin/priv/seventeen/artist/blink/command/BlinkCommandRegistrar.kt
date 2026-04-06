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
package priv.seventeen.artist.blink.command

import org.bukkit.Bukkit
import org.bukkit.command.CommandMap
import org.bukkit.plugin.java.JavaPlugin

object BlinkCommandRegistrar {

    private val commandMap: CommandMap by lazy {
        val server = Bukkit.getServer()
        // 沿类继承链查找 getCommandMap，兼容 Arclight 等混合端
        val method = generateSequence<Class<*>>(server.javaClass) { it.superclass }
            .flatMap { it.declaredMethods.asSequence() }
            .first { it.name == "getCommandMap" && it.parameterCount == 0 }
            .apply { isAccessible = true }
        method.invoke(server) as CommandMap
    }

    fun register(plugin: JavaPlugin, command: BlinkCommand, fallbackPrefix: String = plugin.description.name) {
        commandMap.register(fallbackPrefix, command)
    }
}
