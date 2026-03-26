package com.example.testplugin.util

import org.bukkit.ChatColor

fun colorize(text: String): String = ChatColor.translateAlternateColorCodes('&', text)

fun stripColor(text: String): String = ChatColor.stripColor(text) ?: text
