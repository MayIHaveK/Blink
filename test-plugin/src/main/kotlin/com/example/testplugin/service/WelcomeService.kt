package com.example.testplugin.service

import com.example.testplugin.Settings
import com.example.testplugin.util.colorize
import org.bukkit.entity.Player

object WelcomeService {

    private var welcomeLines = listOf<String>()

    fun init() {
        reload()
    }

    fun reload() {
        welcomeLines = listOf(
            "",
            "&b&l╔══════════════════════════════════╗",
            "&b&l║  &e&l⚡ BlinkTest Server             &b&l║",
            "&b&l╚══════════════════════════════════╝",
            "",
            "  ${Settings.instance.welcomeMessage}",
            ""
        )
    }

    fun sendWelcome(player: Player) {
        for (line in welcomeLines) {
            player.sendMessage(colorize(line))
        }

        if (Settings.instance.showTitle) {
            player.sendTitle(
                colorize("&e&l欢迎"),
                colorize("&7${player.name}"),
                10,
                Settings.instance.titleDuration,
                10
            )
        }
    }
}
