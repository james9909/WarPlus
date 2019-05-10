package com.github.james9909.warplus.command

import com.github.james9909.warplus.WarPlus
import org.bukkit.command.CommandSender

abstract class AbstractCommand(
    val plugin: WarPlus,
    val sender: CommandSender,
    val args: List<String>
) {

    abstract fun handle(): Boolean
}