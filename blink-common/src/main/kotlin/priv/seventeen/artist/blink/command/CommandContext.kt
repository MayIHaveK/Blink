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
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class CommandContext(
    val sender: CommandSender,
    val label: String,
    private val args: Array<String>
) {
    val size: Int get() = args.size
    val isPlayer: Boolean get() = sender is Player
    val player: Player? get() = sender as? Player

    fun arg(index: Int): String = args.getOrElse(index) { "" }

    fun argInt(index: Int, default: Int = 0): Int {
        val raw = arg(index)
        return raw.toIntOrNull() ?: raw.substringBefore('.').toIntOrNull() ?: default
    }

    fun argDouble(index: Int, default: Double = 0.0): Double = arg(index).toDoubleOrNull() ?: default
    fun argFloat(index: Int, default: Float = 0f): Float = arg(index).toFloatOrNull() ?: default
    fun argLong(index: Int, default: Long = 0L): Long = arg(index).toLongOrNull() ?: default

    fun argBoolean(index: Int, default: Boolean = false): Boolean = when (arg(index).lowercase()) {
        "true", "yes", "1" -> true
        "false", "no", "0" -> false
        else -> default
    }

    fun argPlayer(index: Int): Player? = Bukkit.getPlayerExact(arg(index))
    fun argUUID(index: Int): UUID? = try { UUID.fromString(arg(index)) } catch (_: Exception) { null }
    fun argJoined(fromIndex: Int, separator: String = " "): String = args.drop(fromIndex).joinToString(separator)
    fun reply(message: String) { sender.sendMessage(message) }
}
