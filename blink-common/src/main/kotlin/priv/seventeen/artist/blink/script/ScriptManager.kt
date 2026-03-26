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
package priv.seventeen.artist.blink.script

import priv.seventeen.artist.blink.bukkitPlugin
import java.io.File
import java.net.URLClassLoader
import java.util.*
import java.util.logging.Level
import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory

object ScriptManager {

    @Volatile
    private var engineClassLoader: URLClassLoader? = null
    @Volatile
    private var defaultEngine: ScriptEngine? = null

    fun init(nashornJars: List<File>) {
        if (nashornJars.isEmpty()) {
            bukkitPlugin.logger.warning("[Blink] Nashorn JAR 文件为空，JS 引擎不可用")
            return
        }

        val urls = nashornJars.map { it.toURI().toURL() }.toTypedArray()
        engineClassLoader = URLClassLoader(urls, bukkitPlugin.javaClass.classLoader)

        val factories = ServiceLoader.load(ScriptEngineFactory::class.java, engineClassLoader)
        val nashornFactory = factories.firstOrNull {
            it.engineName.contains("nashorn", ignoreCase = true)
        }

        if (nashornFactory == null) {
            bukkitPlugin.logger.warning("[Blink] 未找到 Nashorn 引擎，JS 功能不可用")
            return
        }

        defaultEngine = nashornFactory.scriptEngine
        bukkitPlugin.logger.info("[Blink] JS 引擎已初始化: ${nashornFactory.engineName} ${nashornFactory.engineVersion}")
    }

    val isAvailable: Boolean get() = defaultEngine != null

    fun getEngine(): ScriptEngine {
        return defaultEngine ?: throw IllegalStateException("JS 引擎未初始化，请确认 enableScript 已开启")
    }

    /** 创建独立引擎实例，用于隔离执行环境 */
    fun createEngine(): ScriptEngine {
        val cl = engineClassLoader ?: throw IllegalStateException("JS 引擎未初始化")
        val factories = ServiceLoader.load(ScriptEngineFactory::class.java, cl)
        return factories.first {
            it.engineName.contains("nashorn", ignoreCase = true)
        }.scriptEngine
    }

    /** 共享 defaultEngine，线程安全通过 synchronized 保证；如需并发请用 [createEngine] */
    fun eval(script: String, bindings: Map<String, Any?> = emptyMap()): Any? {
        val eng = getEngine()
        val ctx = eng.createBindings()
        ctx.putAll(bindings)
        return synchronized(eng) { eng.eval(script, ctx) }
    }

    fun evalFile(file: File, bindings: Map<String, Any?> = emptyMap()): Any? {
        return eval(file.readText(Charsets.UTF_8), bindings)
    }

    fun shutdown() {
        defaultEngine = null
        try {
            engineClassLoader?.close()
        } catch (e: Exception) {
            bukkitPlugin.logger.log(Level.WARNING, "[Blink] 关闭 JS 引擎 ClassLoader 时出错", e)
        }
        engineClassLoader = null
    }
}
