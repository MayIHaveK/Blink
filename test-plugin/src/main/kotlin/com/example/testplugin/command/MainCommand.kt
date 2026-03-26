package com.example.testplugin.command

import com.example.testplugin.Settings
import com.example.testplugin.service.CooldownManager
import com.example.testplugin.service.ScriptBridge
import priv.seventeen.artist.blink.bukkitPlugin
import priv.seventeen.artist.blink.command.BlinkCommand
import priv.seventeen.artist.blink.command.BlinkCommandRegistrar
import priv.seventeen.artist.blink.command.SenderType
import priv.seventeen.artist.blink.lifecycle.Awake
import priv.seventeen.artist.blink.lifecycle.LifeCycle

object MainCommand {

    @Awake(LifeCycle.ENABLE, priority = 5)
    fun register() {
        val cmd = BlinkCommand("blinktest", "bt")
            .command("reload", "重载配置", permission = "blinktest.reload") { ctx ->
                Settings.reload()
                ctx.reply("§a配置已重载")
            }
            .command("info", "查看插件信息") { ctx ->
                ctx.reply("§bBlinkTest §fv1.0.0")
                ctx.reply("§7Framework: §fBlink")
                ctx.reply("§7Script: §f${if (ScriptBridge.isAvailable) "可用" else "不可用"}")
                ctx.reply("§7Debug: §f${Settings.instance.debug}")
            }
            .command("eval", "执行 JS 脚本",
                args = arrayOf("script"),
                permission = "blinktest.eval",
                sender = SenderType.OP
            ) { ctx ->
                val script = ctx.argJoined(0)
                if (!ScriptBridge.isAvailable) {
                    ctx.reply("§cJS 引擎不可用")
                    return@command
                }
                try {
                    val result = ScriptBridge.eval(script)
                    ctx.reply("§a结果: §f$result")
                } catch (e: Exception) {
                    ctx.reply("§c执行出错: ${e.message}")
                }
            }
            .command("cooldown", "测试冷却系统", sender = SenderType.PLAYER) { ctx ->
                val player = ctx.player ?: return@command
                if (CooldownManager.isOnCooldown(player.uniqueId, "test")) {
                    val remaining = CooldownManager.remaining(player.uniqueId, "test")
                    ctx.reply("§c冷却中，还剩 ${remaining / 1000}s")
                    return@command
                }
                CooldownManager.setCooldown(player.uniqueId, "test", Settings.instance.commandCooldown * 1000L)
                ctx.reply("§a操作成功，冷却 ${Settings.instance.commandCooldown}s")
            }
            .group(AdminCommands())

        BlinkCommandRegistrar.register(bukkitPlugin, cmd)
    }
}
