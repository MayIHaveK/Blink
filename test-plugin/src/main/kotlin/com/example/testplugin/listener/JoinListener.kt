package com.example.testplugin.listener

import com.example.testplugin.Settings
import com.example.testplugin.service.WelcomeService
import com.example.testplugin.service.ScriptBridge
import priv.seventeen.artist.blink.bukkitPlugin
import priv.seventeen.artist.blink.event.AutoListener
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerJoinEvent

object JoinListener {

    @AutoListener
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        event.joinMessage = "§a+ §e${player.name} §7加入了服务器"

        WelcomeService.sendWelcome(player)

        if (ScriptBridge.isAvailable) {
            Bukkit.getScheduler().runTaskLater(bukkitPlugin, Runnable {
                ScriptBridge.giveWelcomeItem(player.name)
            }, 20L)
        }
    }
}
