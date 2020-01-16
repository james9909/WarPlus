package com.github.james9909.warplus

import com.github.james9909.warplus.command.CommandHandler
import com.github.james9909.warplus.listeners.BlockListener
import com.github.james9909.warplus.listeners.EntityListener
import com.github.james9909.warplus.listeners.PlayerListener
import com.github.james9909.warplus.managers.DatabaseManager
import com.github.james9909.warplus.managers.PlayerManager
import com.github.james9909.warplus.managers.WarzoneManager
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.event.HandlerList
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class WarPlus : JavaPlugin {
    val warzoneManager = WarzoneManager(this)
    val playerManager = PlayerManager(this)
    val databaseManager = DatabaseManager(this, "jdbc:sqlite:$dataFolder/war.db")
    val commandHandler = CommandHandler()
    var loaded = AtomicBoolean()

    constructor() : super()

    /* Used for tests */
    internal constructor(
        loader: JavaPluginLoader,
        description: PluginDescriptionFile,
        dataFolder: File,
        file: File?
    ) : super(loader, description, dataFolder, file ?: File(".")) /* Workaround for MockBukkit */

    override fun onEnable() {
        logger.info("Initializing WarPlus")
        initialize()
        logger.info("Done initializing")
    }

    override fun onDisable() {
        logger.info("Disabling WarPlus")
        disable()
        logger.info("Done disabling")
    }

    fun initialize() {
        if (loaded.get()) {
            return
        }
        setupListeners()
        val dataFolder = dataFolder
        if (!dataFolder.exists()) {
            dataFolder.mkdir()
        }
        reloadConfig()
        warzoneManager.loadWarzones()
        databaseManager.createTables()
        loaded.set(true)
    }

    fun disable() {
        if (!loaded.get()) {
            return
        }
        HandlerList.unregisterAll(this)
        server.scheduler.cancelTasks(this)
        warzoneManager.unloadWarzones()
        loaded.set(false)
    }

    private fun setupListeners() {
        val pluginManager = server.pluginManager
        pluginManager.registerEvents(BlockListener(this), this)
        pluginManager.registerEvents(EntityListener(this), this)
        pluginManager.registerEvents(PlayerListener(this), this)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val warCommand = commandHandler.getCommand(this, sender, args) ?: return false
        return warCommand.handle()
    }
}