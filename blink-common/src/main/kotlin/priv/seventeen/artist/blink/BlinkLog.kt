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
package priv.seventeen.artist.blink

import org.bukkit.Bukkit
import java.util.logging.Level

/**
 * Blink 统一控制台日志输出。
 *
 * <p>蓝色 {@code ◆ Blink} 前缀 + 白色信息文本，错误红色，成功绿色，警告黄色。
 * 通过 {@link Bukkit#getConsoleSender()} 直接发送带颜色码的消息，
 * Bukkit 未初始化时自动回退到 {@code System.out}。
 */
object BlinkLog {

    private const val PREFIX = "\u00A79\u25C6 \u00A7bBlink \u00A78| "
    private val COLOR_PATTERN = Regex("\u00A7[0-9a-fk-or]")

    /**
     * 输出普通信息（白色）。
     *
     * @param message 日志内容
     */
    fun info(message: String) {
        send("$PREFIX\u00A7f$message")
    }

    /**
     * 输出成功信息（绿色）。
     *
     * @param message 日志内容
     */
    fun success(message: String) {
        send("$PREFIX\u00A7a$message")
    }

    /**
     * 输出警告信息（黄色）。
     *
     * @param message 日志内容
     */
    fun warn(message: String) {
        send("$PREFIX\u00A7e$message")
    }

    /**
     * 输出错误信息（红色）。
     *
     * @param message 日志内容
     */
    fun error(message: String) {
        send("$PREFIX\u00A7c$message")
    }

    /**
     * 输出错误信息（红色）并打印异常堆栈。
     * 堆栈通过 {@link java.util.logging.Logger} 输出，避免控制台刷屏。
     *
     * @param message   日志内容
     * @param throwable 异常
     */
    fun error(message: String, throwable: Throwable) {
        send("$PREFIX\u00A7c$message")
        bukkitPlugin.logger.log(Level.WARNING, "", throwable)
    }

    /**
     * 输出次要信息（灰色），用于下载进度等辅助提示。
     *
     * @param message 日志内容
     */
    fun detail(message: String) {
        send("$PREFIX\u00A77$message")
    }

    private fun send(message: String) {
        try {
            Bukkit.getConsoleSender().sendMessage(message)
        } catch (_: Exception) {
            println(message.replace(COLOR_PATTERN, ""))
        }
    }
}
