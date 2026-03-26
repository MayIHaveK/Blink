package com.example.testplugin.command

import com.example.testplugin.service.CooldownManager
import com.example.testplugin.service.WelcomeService
import priv.seventeen.artist.blink.command.BlinkCommandGroup
import priv.seventeen.artist.blink.command.CommandContext
import priv.seventeen.artist.blink.command.SenderType
import priv.seventeen.artist.blink.command.SubCommand

class AdminCommands : BlinkCommandGroup("admin", "管理命令") {

    @SubCommand(
        name = "welcome",
        description = "手动发送欢迎消息",
        args = ["player"],
        permission = "blinktest.admin",
        sender = SenderType.OP
    )
    fun welcome(ctx: CommandContext) {
        val target = ctx.argPlayer(0) ?: return ctx.reply("§c玩家不在线")
        WelcomeService.sendWelcome(target)
        ctx.reply("§a已向 ${target.name} 发送欢迎消息")
    }

    @SubCommand(
        name = "resetcd",
        description = "重置玩家冷却",
        args = ["player"],
        permission = "blinktest.admin"
    )
    fun resetCooldown(ctx: CommandContext) {
        val target = ctx.argPlayer(0) ?: return ctx.reply("§c玩家不在线")
        CooldownManager.clearAll(target.uniqueId)
        ctx.reply("§a已重置 ${target.name} 的所有冷却")
    }

    @SubCommand(name = "debug", description = "切换调试模式", permission = "blinktest.admin")
    fun toggleDebug(ctx: CommandContext) {
        val cfg = com.example.testplugin.Settings.instance
        cfg.debug = !cfg.debug
        ctx.reply("§7调试模式: ${if (cfg.debug) "§a开启" else "§c关闭"}")
    }
}
