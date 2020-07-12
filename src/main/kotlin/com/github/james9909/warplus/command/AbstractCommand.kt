package com.github.james9909.warplus.command

import com.github.james9909.warplus.WarPlus
import org.bukkit.command.CommandSender

abstract class AbstractCommand {
    abstract val USAGE_STRING: String
    abstract val DESCRIPTION: String

    abstract fun execute(plugin: WarPlus, sender: CommandSender, args: List<String>): Boolean
    abstract fun tab(plugin: WarPlus, sender: CommandSender, args: List<String>): List<String>
}