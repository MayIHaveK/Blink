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

import java.lang.invoke.MethodHandles

abstract class BlinkCommandGroup(
    val groupName: String,
    val groupDescription: String = ""
) {
    internal val commands = linkedMapOf<String, ResolvedCommand>()
    private val nameMap = hashMapOf<String, String>()

    init { scanCommands() }

    internal fun resolve(name: String): ResolvedCommand? = commands[nameMap[name.lowercase()]]

    private fun scanCommands() {
        val lookup = MethodHandles.privateLookupIn(this.javaClass, MethodHandles.lookup())
        for (method in this.javaClass.declaredMethods) {
            val ann = method.getAnnotation(SubCommand::class.java) ?: continue
            require(method.parameterCount == 1 && method.parameterTypes[0] == CommandContext::class.java) {
                "@SubCommand ${javaClass.simpleName}#${method.name} 参数必须为 (CommandContext)"
            }
            method.isAccessible = true
            val handle = lookup.unreflect(method).bindTo(this)
            commands[ann.name] = ResolvedCommand(
                ann.name, ann.description, ann.permission, ann.args, ann.sender, handle, this
            )
            nameMap[ann.name.lowercase()] = ann.name
        }
    }
}
