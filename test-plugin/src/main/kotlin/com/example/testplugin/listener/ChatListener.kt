package com.example.testplugin.listener

import com.example.testplugin.Settings
import com.example.testplugin.util.colorize
import priv.seventeen.artist.blink.bukkitPlugin
import priv.seventeen.artist.blink.event.AutoListener
import org.bukkit.event.EventPriority
import org.bukkit.event.player.AsyncPlayerChatEvent

object ChatListener {

    @AutoListener(priority = EventPriority.HIGH)
    fun onChat(event: AsyncPlayerChatEvent) {
        if (Settings.instance.debug) {
            bukkitPlugin.logger.info("[Debug] ${event.player.name}: ${event.message}")
        }

        event.format = "§7${event.player.name}§8: §f${event.message}"
    }
}
