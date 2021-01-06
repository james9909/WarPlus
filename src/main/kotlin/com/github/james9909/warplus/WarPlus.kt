package com.github.james9909.warplus

import com.github.james9909.warplus.command.CommandHandler
import com.github.james9909.warplus.config.DatabaseDialect
import com.github.james9909.warplus.config.TeamConfigType
import com.github.james9909.warplus.config.WarConfigType
import com.github.james9909.warplus.config.WarzoneConfigType
import com.github.james9909.warplus.extensions.get
import com.github.james9909.warplus.listeners.BlockListener
import com.github.james9909.warplus.listeners.EntityListener
import com.github.james9909.warplus.listeners.FaweListener
import com.github.james9909.warplus.listeners.MagicSpellsListener
import com.github.james9909.warplus.listeners.PlayerListener
import com.github.james9909.warplus.managers.ClassManager
import com.github.james9909.warplus.managers.DatabaseManager
import com.github.james9909.warplus.managers.InventoryManager
import com.github.james9909.warplus.managers.ItemNameManager
import com.github.james9909.warplus.managers.PlayerManager
import com.github.james9909.warplus.managers.WarzoneManager
import com.github.james9909.warplus.runnable.UpdateScoreboardRunnable
import com.github.james9909.warplus.sql.MySqlConnectionFactory
import com.github.james9909.warplus.sql.SqliteConnectionFactory
import com.sk89q.worldedit.WorldEdit
import net.milkbowl.vault.economy.Economy
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.HandlerList
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import org.yaml.snakeyaml.error.YAMLException
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

const val WARPLUS_BASE_COMMAND = "wp"

const val UPDATE_SCOREBOARD_TICKS = 10L

val DEFAULT_TEAM_CONFIG by lazy {
    val config = YamlConfiguration()
    config[TeamConfigType.DEFAULT_CLASS.path] = TeamConfigType.DEFAULT_CLASS.default
    config[TeamConfigType.ECON_REWARD.path] = TeamConfigType.ECON_REWARD.default
    config[TeamConfigType.LIVES.path] = TeamConfigType.LIVES.default
    config[TeamConfigType.MAX_PLAYERS.path] = TeamConfigType.MAX_PLAYERS.default
    config[TeamConfigType.MAX_SCORE.path] = TeamConfigType.MAX_SCORE.default
    config[TeamConfigType.MIN_PLAYERS.path] = TeamConfigType.MIN_PLAYERS.default
    config[TeamConfigType.HUNGER.path] = TeamConfigType.HUNGER.default
    config[TeamConfigType.PLACE_BLOCKS.path] = TeamConfigType.PLACE_BLOCKS.default
    config[TeamConfigType.SPAWN_STYLE.path] = TeamConfigType.SPAWN_STYLE.default
    config
}

val DEFAULT_WARZONE_CONFIG by lazy {
    val config = YamlConfiguration()
    config[WarzoneConfigType.BLOCK_HEADS.path] = WarzoneConfigType.BLOCK_HEADS.default
    config[WarzoneConfigType.CAN_BREAK_BLOCKS.path] = WarzoneConfigType.CAN_BREAK_BLOCKS.default
    config[WarzoneConfigType.CLASS_CMD.path] = WarzoneConfigType.CLASS_CMD.default
    config[WarzoneConfigType.DEATH_MESSAGES.path] = WarzoneConfigType.DEATH_MESSAGES.default
    config[WarzoneConfigType.ENABLED.path] = WarzoneConfigType.ENABLED.default
    config[WarzoneConfigType.MAX_HEALTH.path] = WarzoneConfigType.MAX_HEALTH.default
    config[WarzoneConfigType.MIN_TEAMS.path] = WarzoneConfigType.MIN_TEAMS.default
    config[WarzoneConfigType.MONUMENT_HEAL.path] = WarzoneConfigType.MONUMENT_HEAL.default
    config[WarzoneConfigType.MONUMENT_HEAL_CHANCE.path] = WarzoneConfigType.MONUMENT_HEAL_CHANCE.default
    config[WarzoneConfigType.ITEM_DROPS.path] = WarzoneConfigType.ITEM_DROPS.default
    config[WarzoneConfigType.REMOVE_ENTITIES_ON_RESET.path] = WarzoneConfigType.REMOVE_ENTITIES_ON_RESET.default
    config[WarzoneConfigType.RESET_ON_EMPTY.path] = WarzoneConfigType.RESET_ON_EMPTY.default
    config[WarzoneConfigType.CAPTURE_POINT_TIME.path] = WarzoneConfigType.CAPTURE_POINT_TIME.default
    config
}

class WarPlus : JavaPlugin {
    val classManager = ClassManager(this)
    val warzoneManager = WarzoneManager(this)
    val playerManager = PlayerManager(this)
    val inventoryManager = InventoryManager(this)
    val itemNameManager = ItemNameManager(this)
    var databaseManager: DatabaseManager? = null // late initialization
        private set
    private var usr = UpdateScoreboardRunnable(this)
    private val allowedCommands = mutableSetOf<String>()
    private var worldEditSubscriber: FaweListener? = null // late initialization
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
        val configFile = File(dataFolder, "config.yml")
        if (!configFile.exists()) {
            saveDefaultConfig()
        }
        try {
            reloadConfig()
        } catch (e: YAMLException) {
            logger.warning("Failed to load config: $e")
        }
        allowedCommands.clear()
        allowedCommands.add("/$WARPLUS_BASE_COMMAND")
        config.get(WarConfigType.ALLOWED_WARZONE_COMMANDS).forEach {
            allowedCommands.add(it.trim().toLowerCase())
        }

        setupDatabase()
        classManager.loadClasses()
        warzoneManager.loadWarzones()
        itemNameManager.loadItemNames()
        getCommand(WARPLUS_BASE_COMMAND)?.setExecutor(CommandHandler(this))
        setupRunnables()
        setupEconomy()
        loaded.set(true)
    }

    fun disable() {
        if (!loaded.get()) {
            return
        }
        HandlerList.unregisterAll(this)
        server.scheduler.cancelTasks(this)
        warzoneManager.unloadWarzones()
        databaseManager?.close()
        itemNameManager.clear()
        cancelRunnables()
        if (server.name != "ServerMock") {
            WorldEdit.getInstance().eventBus.unregister(worldEditSubscriber)
        }
        loaded.set(false)
    }

    fun hasPlugin(plugin: String): Boolean = server.pluginManager.isPluginEnabled(plugin)

    private fun setupRunnables() {
        usr.runTaskTimer(this, 0, UPDATE_SCOREBOARD_TICKS)
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
        if (server.name != "ServerMock") {
            pluginManager.registerEvents(MagicSpellsListener(this), this)
            worldEditSubscriber = FaweListener(this)
            WorldEdit.getInstance().eventBus.register(worldEditSubscriber)
        }
    }

    private fun setupEconomy() {
        if (!hasPlugin("Vault")) {
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

    private fun setupDatabase() {
        this.databaseManager = if (!config.get(WarConfigType.DATABASE_ENABLED)) {
            logger.info("Not using a database for storage")
            null
        } else {
            try {
                when (config.get(WarConfigType.DATABASE_DIALECT)) {
                    DatabaseDialect.MYSQL -> {
                        logger.info("Enabling MySQL backend")
                        DatabaseManager(
                            this,
                            MySqlConnectionFactory(
                                config.get(WarConfigType.DATABASE_SERVER),
                                config.get(WarConfigType.DATABASE_PORT),
                                config.get(WarConfigType.DATABASE_NAME),
                                config.get(WarConfigType.DATABASE_USERNAME),
                                config.get(WarConfigType.DATABASE_PASSWORD)
                            )
                        )
                    }
                    DatabaseDialect.SQLITE -> {
                        logger.info("Enabling SQLite backend")
                        DatabaseManager(
                            this,
                            SqliteConnectionFactory(
                                "$dataFolder/${config.get(WarConfigType.DATABASE_FILENAME)}"
                            )
                        )
                    }
                }
            } catch (e: IllegalArgumentException) {
                logger.warning("Unsupported database dialect. Accepted values are: [mysql, sqlite]")
                null
            }
        }
        databaseManager?.let { dbm ->
            server.scheduler.runTaskAsynchronously(this) { _ ->
                dbm.init()
                dbm.createTables()
            }
        }
    }

    fun canExecuteCommand(command: String): Boolean {
        val args = command.toLowerCase().split(" ")
        if (args.isNotEmpty() && allowedCommands.contains(args[0])) {
            return true
        }
        return allowedCommands.contains(args.joinToString(separator = " "))
    }
}
