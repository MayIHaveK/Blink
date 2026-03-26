package com.example.testplugin.service

import priv.seventeen.artist.blink.bukkitPlugin
import priv.seventeen.artist.blink.loader.DependencyLoader
import priv.seventeen.artist.blink.script.ScriptManager
import org.bukkit.Bukkit

object ScriptBridge {

    val isAvailable: Boolean get() = ScriptManager.isAvailable

    fun init() {
        Bukkit.getScheduler().runTaskAsynchronously(bukkitPlugin, Runnable {
            val jars = DependencyLoader.loadNashorn(bukkitPlugin)
            ScriptManager.init(jars)

            Bukkit.getScheduler().runTask(bukkitPlugin, Runnable {
                if (ScriptManager.isAvailable) {
                    bukkitPlugin.logger.info("[Script] Nashorn ready")
                    val result = ScriptManager.eval("1 + 2 + 3")
                    bukkitPlugin.logger.info("[Script] eval('1 + 2 + 3') = $result")
                } else {
                    bukkitPlugin.logger.warning("[Script] Nashorn not available")
                }
            })
        })
    }

    fun eval(script: String): Any? = ScriptManager.eval(script)

    fun giveWelcomeItem(playerName: String) {
        if (!isAvailable) return

        val script = """
            var Bukkit = Java.type('org.bukkit.Bukkit');
            var Material = Java.type('org.bukkit.Material');
            var ItemStack = Java.type('org.bukkit.inventory.ItemStack');
            var Arrays = Java.type('java.util.Arrays');
            
            var player = Bukkit.getPlayer(playerName);
            if (player != null) {
                var item = new ItemStack(Material.DIAMOND, 1);
                var meta = item.getItemMeta();
                meta.setDisplayName('\u00a7b\u00a7l\u6b22\u8fce\u94bb\u77f3');
                meta.setLore(Arrays.asList([
                    '\u00a77Created by Nashorn JS',
                    '\u00a77via Blink Framework'
                ]));
                item.setItemMeta(meta);
                player.getInventory().addItem(item);
                'gave item to ' + playerName;
            } else {
                'player not found';
            }
        """.trimIndent()

        try {
            ScriptManager.eval(script, mapOf("playerName" to playerName))
        } catch (e: Exception) {
            bukkitPlugin.logger.warning("[Script] giveWelcomeItem failed: ${e.message}")
        }
    }

    fun shutdown() {
        if (isAvailable) {
            ScriptManager.shutdown()
        }
    }
}
