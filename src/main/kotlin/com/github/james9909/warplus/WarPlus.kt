package com.github.james9909.warplus

import com.github.james9909.warplus.command.CommandHandler
import com.github.james9909.warplus.managers.PlayerManager
import com.github.james9909.warplus.managers.WarzoneManager
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import java.io.File

class WarPlus : JavaPlugin {
    val warzoneManager = WarzoneManager(this)
    val playerManager = PlayerManager(this)
    val commandHandler = CommandHandler()

    constructor() : super()

    /* Used for tests */
    internal constructor(
        loader: JavaPluginLoader,
        description: PluginDescriptionFile,
        dataFolder: File,
        file: File?
    ) : super(loader, description, dataFolder, file ?: File(".")) /* Workaround for MockBukkit */

    override fun onEnable() {
        logger.info("Initializing warplus")
        initialize()
        logger.info("Done initializing")
    }

    private fun initialize() {
        val dataFolder = dataFolder
        if (!dataFolder.exists()) {
            dataFolder.mkdir()
        }
        reloadConfig()
        warzoneManager.loadWarzones()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        val candidates = commandHandler.getCommands(args)
        if (candidates.isEmpty()) {
            return false
        }
        return candidates[0].handle()
    }
}