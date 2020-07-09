package com.github.james9909.warplus

import com.github.james9909.warplus.command.CommandHandler
import com.github.james9909.warplus.config.TeamConfigType
import com.github.james9909.warplus.config.WarzoneConfigType
import com.github.james9909.warplus.listeners.BlockListener
import com.github.james9909.warplus.listeners.EntityListener
import com.github.james9909.warplus.listeners.MagicSpellsListener
import com.github.james9909.warplus.listeners.PlayerListener
import com.github.james9909.warplus.managers.ClassManager
import com.github.james9909.warplus.managers.DatabaseManager
import com.github.james9909.warplus.managers.PlayerManager
import com.github.james9909.warplus.managers.WarzoneManager
import com.github.james9909.warplus.runnable.UpdateScoreboardRunnable
import net.milkbowl.vault.economy.Economy
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.HandlerList
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

val DEFAULT_TEAM_CONFIG by lazy {
    val config = YamlConfiguration()
    config[TeamConfigType.LIVES.path] = TeamConfigType.LIVES.default
    config[TeamConfigType.MIN_PLAYERS.path] = TeamConfigType.MIN_PLAYERS.default
    config[TeamConfigType.MAX_PLAYERS.path] = TeamConfigType.MAX_PLAYERS.default
    config[TeamConfigType.MAX_SCORE.path] = TeamConfigType.MAX_SCORE.default
    config[TeamConfigType.SPAWN_STYLE.path] = TeamConfigType.SPAWN_STYLE.default
    config
}

val DEFAULT_WARZONE_CONFIG by lazy {
    val config = YamlConfiguration()
    config[WarzoneConfigType.CLASS_CMD.path] = WarzoneConfigType.CLASS_CMD.default
    config[WarzoneConfigType.DEATH_MESSAGES.path] = WarzoneConfigType.DEATH_MESSAGES.default
    config[WarzoneConfigType.DEFAULT_CLASS.path] = WarzoneConfigType.DEFAULT_CLASS.default
    config[WarzoneConfigType.ENABLED.path] = WarzoneConfigType.ENABLED.default
    config[WarzoneConfigType.MAX_HEALTH.path] = WarzoneConfigType.MAX_HEALTH.default
    config[WarzoneConfigType.MIN_TEAMS.path] = WarzoneConfigType.MIN_TEAMS.default
    config
}

class WarPlus : JavaPlugin {
    val classManager = ClassManager(this)
    val warzoneManager = WarzoneManager(this)
    val playerManager = PlayerManager(this)
    private val databaseManager = DatabaseManager(this, "jdbc:sqlite:$dataFolder/war.db")
    private var usr = UpdateScoreboardRunnable(this)
    var loaded = AtomicBoolean()
        private set
    var economy: Economy? = null
        private set

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
        getCommand("war")?.setExecutor(CommandHandler(this))
        setupRunnables()
        setupEconomy()
        setupMagicspells()
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

    fun hasPlugin(plugin: String): Boolean = server.pluginManager.isPluginEnabled(plugin)

    private fun setupRunnables() {
        usr.runTaskTimerAsynchronously(this, 0, 10)
    }

    private fun cancelRunnables() {
        usr.cancel()
        usr = UpdateScoreboardRunnable(this)
    }

    private fun setupListeners() {
        val pluginManager = server.pluginManager
        pluginManager.registerEvents(BlockListener(this), this)
        pluginManager.registerEvents(EntityListener(this), this)
        pluginManager.registerEvents(PlayerListener(this), this)
    }

    private fun setupEconomy() {
        if (server.pluginManager.getPlugin("Vault") == null) {
            logger.info("Vault not found, economy rewards disabled")
            return
        }
        val e = server.servicesManager.getRegistration(Economy::class.java)
        if (e != null) {
            economy = e.provider
            logger.info("Vault found, economy rewards enabled")
        } else {
            logger.info("Vault found, but no economy plugin was detected. Economy rewards will be disabled")
        }
    }

    private fun setupMagicspells() {
        if (server.pluginManager.getPlugin("MagicSpells") == null) {
            return
        }
        server.pluginManager.registerEvents(MagicSpellsListener(this), this)
    }
}