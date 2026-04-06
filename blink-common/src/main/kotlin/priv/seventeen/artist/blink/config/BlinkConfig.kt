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

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import priv.seventeen.artist.blink.BlinkLog
import java.io.File
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.nio.charset.StandardCharsets
import java.nio.file.Files

abstract class BlinkConfig(val plugin: JavaPlugin, pathName: String) {

    val pathName: String = if (pathName.replace('\\', '/').endsWith(".yml")) pathName.replace('\\', '/') else "${pathName.replace('\\', '/')}.yml"
    @Ignore
    var configFile: File = File(plugin.dataFolder, this.pathName); private set

    fun load() {
        configFile = File(plugin.dataFolder, pathName)
        configFile.parentFile?.mkdirs()
        if (!configFile.exists()) {
            val res = plugin.getResource("assets/$pathName")
            if (res != null) res.use { i -> Files.newOutputStream(configFile.toPath()).use { o -> i.copyTo(o) } }
            else writeDefaults()
        }
        reload()
    }

    fun reload() {
        try { loadFields(this, YamlConfiguration.loadConfiguration(configFile)) }
        catch (e: Exception) { BlinkLog.error("加载配置 $pathName 失败", e) }
    }

    fun save() {
        try {
            val sb = StringBuilder(); writeObj(this, sb, 0)
            Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8).use { it.write(sb.toString()) }
        } catch (e: Exception) { BlinkLog.error("保存配置 $pathName 失败", e) }
    }

    private fun getFieldValue(getter: MethodHandle, field: Field, target: Any): Any? {
        return if (Modifier.isStatic(field.modifiers)) field.get(null) else getter.invoke(target)
    }

    private fun setFieldValue(setter: MethodHandle, field: Field, target: Any, value: Any?) {
        if (Modifier.isStatic(field.modifiers)) field.set(null, value) else setter.invoke(target, value)
    }

    private fun loadFields(target: Any, section: ConfigurationSection) {
        val lookup = MethodHandles.privateLookupIn(target.javaClass, MethodHandles.lookup())
        for (field in collectFields(target.javaClass)) {
            field.isAccessible = true
            if (field.isAnnotationPresent(Ignore::class.java)) continue
            val key = field.getAnnotation(ConfigKey::class.java)?.value ?: field.name
            try {
                val getter = lookup.unreflectGetter(field)
                val setter = lookup.unreflectSetter(field)
                val cur = getFieldValue(getter, field, target)
                if (cur is BlinkSection) { section.getConfigurationSection(key)?.let { loadFields(cur, it) }; continue }
                if (cur is MutableMap<*, *> && isMapOfSection(field)) { loadSectionMap(target, field, key, section); continue }
                loadPrimitive(target, setter, field, key, section, cur)
            } catch (e: Exception) { BlinkLog.error("字段 $pathName.$key 失败", e) }
        }
    }

    private fun loadPrimitive(target: Any, setter: MethodHandle, field: Field, key: String, section: ConfigurationSection, default: Any?) {
        val value = section.get(key, default) ?: return
        when (default) {
            is Int -> setFieldValue(setter, field, target, (value as? Number)?.toInt() ?: default)
            is Long -> setFieldValue(setter, field, target, (value as? Number)?.toLong() ?: default)
            is Double -> setFieldValue(setter, field, target, (value as? Number)?.toDouble() ?: default)
            is Float -> setFieldValue(setter, field, target, (value as? Number)?.toFloat() ?: default)
            is Boolean -> setFieldValue(setter, field, target, value as? Boolean ?: default)
            is String -> setFieldValue(setter, field, target, (section.getString(key, default) ?: default).replace("</n/>", "\n"))
            is List<*> -> {
                val list = section.getList(key)
                if (list != null) setFieldValue(setter, field, target, list)
                else section.getString(key)?.let { setFieldValue(setter, field, target, mutableListOf(it)) }
            }
            else -> setFieldValue(setter, field, target, value)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadSectionMap(target: Any, field: Field, key: String, section: ConfigurationSection) {
        val sub = section.getConfigurationSection(key) ?: return
        val lookup = MethodHandles.privateLookupIn(target.javaClass, MethodHandles.lookup())
        val getter = lookup.unreflectGetter(field)
        val map = getFieldValue(getter, field, target) as MutableMap<String, Any>; map.clear()
        val vType = (field.genericType as ParameterizedType).actualTypeArguments[1] as Class<*>
        val constructor = MethodHandles.lookup().unreflectConstructor(vType.getConstructor())
        for (k in sub.getKeys(false)) {
            val s = sub.getConfigurationSection(k) ?: continue
            val inst = constructor.invoke() as BlinkSection
            loadFields(inst, s); map[k] = inst
        }
    }

    private fun writeDefaults() {
        val sb = StringBuilder(); writeObj(this, sb, 0)
        try { Files.newBufferedWriter(configFile.toPath(), StandardCharsets.UTF_8).use { it.write(sb.toString()) } }
        catch (e: Exception) { BlinkLog.error("写入默认配置 $pathName 失败", e) }
    }

    private fun writeObj(target: Any, sb: StringBuilder, indent: Int) {
        val tab = "  ".repeat(indent); val nl = System.lineSeparator()
        val lookup = MethodHandles.privateLookupIn(target.javaClass, MethodHandles.lookup())
        writeComments(target.javaClass.annotations, sb, tab, nl)
        for (field in collectFields(target.javaClass)) {
            field.isAccessible = true
            if (field.isAnnotationPresent(Ignore::class.java)) continue
            val key = field.getAnnotation(ConfigKey::class.java)?.value ?: field.name
            writeComments(field.annotations, sb, tab, nl)
            val getter = lookup.unreflectGetter(field)
            val v = getFieldValue(getter, field, target)
            when {
                v is BlinkSection -> { sb.append(tab).append(key).append(":").append(nl); writeObj(v, sb, indent + 1) }
                v is List<*> -> { sb.append(tab).append(key).append(":").append(nl); v.forEach { sb.append(tab).append("  - ").append(if (it is String) "\"$it\"" else it).append(nl) } }
                v is String && v.contains("\n") -> { sb.append(tab).append(key).append(": |").append(nl); v.split("\n").forEach { sb.append(tab).append("  ").append(it).append(nl) } }
                v is String -> sb.append(tab).append(key).append(": \"").append(v.replace("\\", "\\\\").replace("\"", "\\\"")).append("\"").append(nl)
                v is Map<*, *> -> { sb.append(tab).append(key).append(":").append(nl); v.forEach { (mk, mv) -> if (mv is BlinkSection) { sb.append(tab).append("  ").append(mk).append(":").append(nl); writeObj(mv, sb, indent + 2) } else sb.append(tab).append("  ").append(mk).append(": \"").append(mv).append("\"").append(nl) } }
                else -> sb.append(tab).append(key).append(": ").append(v).append(nl)
            }
        }
    }

    private fun writeComments(anns: Array<Annotation>, sb: StringBuilder, tab: String, nl: String) {
        anns.filterIsInstance<Comment>().forEach { sb.append(tab).append("# ").append(it.value).append(nl) }
    }

    private fun collectFields(clazz: Class<*>): List<Field> {
        val fields = mutableListOf<Field>()
        val isObjectSingleton = try {
            clazz.getDeclaredField("INSTANCE")
            true
        } catch (_: NoSuchFieldException) { false }

        var c: Class<*>? = clazz
        while (c != null && c != BlinkConfig::class.java && c != Any::class.java) {
            fields.addAll(c.declaredFields)
            c = c.superclass
        }
        return fields.filter {
            if (Modifier.isTransient(it.modifiers) || Modifier.isFinal(it.modifiers)) return@filter false
            if (it.name == "INSTANCE" || it.isSynthetic) return@filter false
            if (Modifier.isStatic(it.modifiers) && !isObjectSingleton) return@filter false
            true
        }
    }

    private fun isMapOfSection(field: Field): Boolean {
        val t = field.genericType
        if (t is ParameterizedType && t.actualTypeArguments.size == 2 && t.actualTypeArguments[0] == String::class.java) {
            val vt = t.actualTypeArguments[1]
            return vt is Class<*> && BlinkSection::class.java.isAssignableFrom(vt)
        }
        return false
    }
}
