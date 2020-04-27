package com.github.james9909.warplus

import com.github.james9909.warplus.command.CommandHandler
import com.github.james9909.warplus.listeners.BlockListener
import com.github.james9909.warplus.listeners.EntityListener
import com.github.james9909.warplus.listeners.PlayerListener
import com.github.james9909.warplus.managers.ClassManager
import com.github.james9909.warplus.managers.DatabaseManager
import com.github.james9909.warplus.managers.PlayerManager
import com.github.james9909.warplus.managers.WarzoneManager
import com.github.james9909.warplus.runnable.UpdateScoreboardRunnable
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.event.HandlerList
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class WarPlus : JavaPlugin {
    val classManager = ClassManager(this)
    val warzoneManager = WarzoneManager(this)
    val playerManager = PlayerManager(this)
    private val databaseManager = DatabaseManager(this, "jdbc:sqlite:$dataFolder/war.db")
    private val commandHandler = CommandHandler()
    private val usr = UpdateScoreboardRunnable(this)
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
        classManager.loadClasses()
        warzoneManager.loadWarzones()
        databaseManager.createTables()
        setupRunnables()
        loaded.set(true)
    }

    fun disable() {
        if (!loaded.get()) {
            return
        }
        HandlerList.unregisterAll(this)
        server.scheduler.cancelTasks(this)
        warzoneManager.unloadWarzones()
        cancelRunnables()
        loaded.set(false)
    }

    private fun setupRunnables() {
        usr.runTaskTimerAsynchronously(this, 0, 10)
    }

    private fun cancelRunnables() {
        usr.cancel()
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