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
package priv.seventeen.artist.blink.config

import org.bukkit.plugin.java.JavaPlugin
import priv.seventeen.artist.blink.BlinkLog
import java.io.File

abstract class BlinkConfigFolder<T : BlinkConfig>(private val plugin: JavaPlugin, folderName: String) {

    val folderName: String = if (folderName.endsWith("/")) folderName else "$folderName/"
    private val folder: File = File(plugin.dataFolder, this.folderName)
    val configs = linkedMapOf<String, T>()

    protected abstract fun createConfig(plugin: JavaPlugin, filePath: String): T
    protected open fun onCreateFolder(plugin: JavaPlugin, folderPath: String) {}

    fun load() {
        if (!folder.exists() && folder.mkdirs()) onCreateFolder(plugin, folderName)
        reload()
    }

    fun reload() {
        configs.clear()
        val paths = mutableSetOf<String>()
        collectFiles(folder, folder, paths)
        for (path in paths) {
            if (!path.endsWith(".yml")) continue
            val id = path.removeSuffix(".yml").replace(File.separatorChar, '/')
            try { val c = createConfig(plugin, folderName + path); c.load(); configs[id] = c }
            catch (e: Exception) { BlinkLog.error("加载 $folderName$path 失败", e) }
        }
    }

    private fun collectFiles(dir: File, root: File, paths: MutableSet<String>) {
        dir.listFiles()?.forEach {
            if (it.isDirectory) collectFiles(it, root, paths)
            else paths.add(root.toPath().relativize(it.toPath()).toString())
        }
    }
}
