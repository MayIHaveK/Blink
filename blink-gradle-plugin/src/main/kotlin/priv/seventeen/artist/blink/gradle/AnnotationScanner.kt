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
package priv.seventeen.artist.blink.gradle

import org.objectweb.asm.*
import java.io.File

class AnnotationScanner {

    data class AwakeEntry(
        val ownerInternal: String,
        val methodName: String,
        val methodDesc: String,
        val isStatic: Boolean,
        val hasInstance: Boolean,
        val lifeCycle: String,
        val priority: Int
    )

    data class ListenerEntry(
        val ownerInternal: String,
        val methodName: String,
        val methodDesc: String,
        val isStatic: Boolean,
        val hasInstance: Boolean,
        val eventTypeInternal: String,
        val priority: String,
        val ignoreCancelled: Boolean
    )

    val awakeEntries = mutableListOf<AwakeEntry>()
    val listenerEntries = mutableListOf<ListenerEntry>()

    fun scan(classesRoot: File) {
        classesRoot.walk()
            .filter { it.isFile && it.extension == "class" }
            .filter { !it.nameWithoutExtension.startsWith("BlinkGenerated") }
            .forEach { scanClassFile(it) }
    }

    private fun scanClassFile(file: File) {
        val reader = ClassReader(file.readBytes())
        val visitor = BlinkClassVisitor()
        reader.accept(visitor, ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
    }

    private inner class BlinkClassVisitor : ClassVisitor(Opcodes.ASM9) {
        private var className = ""
        private var hasInstanceField = false

        override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<String>?) {
            className = name
            hasInstanceField = false
        }

        override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor? {
            if (name == "INSTANCE" && (access and Opcodes.ACC_STATIC) != 0 && descriptor == "L$className;") {
                hasInstanceField = true
            }
            return null
        }

        override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<String>?): MethodVisitor {
            val isStatic = (access and Opcodes.ACC_STATIC) != 0
            return BlinkMethodVisitor(className, name, descriptor, isStatic, this)
        }

        fun isObjectSingleton() = hasInstanceField
    }

    private inner class BlinkMethodVisitor(
        private val ownerInternal: String,
        private val methodName: String,
        private val methodDesc: String,
        private val isStatic: Boolean,
        private val classVisitor: BlinkClassVisitor
    ) : MethodVisitor(Opcodes.ASM9) {

        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            return when (descriptor) {
                AWAKE_DESC -> AwakeAnnotationVisitor()
                AUTO_LISTENER_DESC -> AutoListenerAnnotationVisitor()
                else -> null
            }
        }

        private inner class AwakeAnnotationVisitor : AnnotationVisitor(Opcodes.ASM9) {
            private var lifeCycle = "ENABLE"
            private var priority = 0

            override fun visitEnum(name: String, descriptor: String, value: String) {
                if (name == "value") lifeCycle = value
            }

            override fun visit(name: String, value: Any) {
                if (name == "priority" && value is Int) priority = value
            }

            override fun visitEnd() {
                if (methodDesc != "()V") {
                    System.err.println("[Blink] WARNING: @Awake method $ownerInternal#$methodName has descriptor $methodDesc, expected ()V — skipping")
                    return
                }
                awakeEntries.add(AwakeEntry(
                    ownerInternal, methodName, methodDesc, isStatic,
                    classVisitor.isObjectSingleton(), lifeCycle, priority
                ))
            }
        }

        private inner class AutoListenerAnnotationVisitor : AnnotationVisitor(Opcodes.ASM9) {
            private var priority = "NORMAL"
            private var ignoreCancelled = false

            override fun visitEnum(name: String, descriptor: String, value: String) {
                if (name == "priority") priority = value
            }

            override fun visit(name: String, value: Any) {
                if (name == "ignoreCancelled" && value is Boolean) ignoreCancelled = value
            }

            override fun visitEnd() {
                val eventType = extractFirstParamType(methodDesc)
                if (eventType == null) {
                    System.err.println("[Blink] WARNING: @AutoListener method $ownerInternal#$methodName has no valid Event parameter — skipping")
                    return
                }
                listenerEntries.add(ListenerEntry(
                    ownerInternal, methodName, methodDesc, isStatic,
                    classVisitor.isObjectSingleton(), eventType, priority, ignoreCancelled
                ))
            }
        }
    }

    companion object {
        private const val AWAKE_DESC = "Lpriv/seventeen/artist/blink/lifecycle/Awake;"
        private const val AUTO_LISTENER_DESC = "Lpriv/seventeen/artist/blink/event/AutoListener;"

        fun extractFirstParamType(desc: String): String? {
            val start = desc.indexOf('(')
            val end = desc.indexOf(')')
            if (start < 0 || end < 0 || end <= start + 1) return null
            val params = desc.substring(start + 1, end)
            if (params.startsWith('L')) {
                val semi = params.indexOf(';')
                if (semi > 0) return params.substring(1, semi)
            }
            return null
        }
    }
}
