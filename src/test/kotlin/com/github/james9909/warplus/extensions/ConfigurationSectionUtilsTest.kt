package com.github.james9909.warplus.extensions

import be.seeseemelk.mockbukkit.MockBukkit
import com.github.james9909.warplus.WarPlus
import com.github.james9909.warplus.config.TeamConfigType
import com.github.james9909.warplus.config.WarzoneConfigType
import com.github.james9909.warplus.structures.SpawnStyle
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigurationSectionUtilsTest {
    private val server = MockBukkit.mock()
    private val plugin = MockBukkit.load(WarPlus::class.java)

    init {
        server.addSimpleWorld("flat")
    }

    @AfterAll
    internal fun afterAll() {
        MockBukkit.unmock()
    }

    @Test
    fun `can use get() with ConfigKey`() {
        val configFile = File("src/test/resources/fixtures/config/warzone-valid.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        val warzoneConfig = config.getConfigurationSection("settings")
        require(warzoneConfig != null)
        assert(warzoneConfig.get(WarzoneConfigType.ENABLED))
        assert(warzoneConfig.get(WarzoneConfigType.MIN_TEAMS) == 1)
    }

    @Test
    fun `get() correctly retrieves default ConfigKey values`() {
        val config = YamlConfiguration()
        assert(config.get(WarzoneConfigType.ENABLED))
        assert(config.get(WarzoneConfigType.MIN_TEAMS) == 2)
        assert(config.get(WarzoneConfigType.CLASS_CMD) == "")
    }

    @Test
    fun `get() correctly retrieves default ConfigKey values for enums`() {
        val configFile = File("src/test/resources/fixtures/config/warzone-valid.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        val blueConfig = config.getConfigurationSection("teams.blue.settings")
        require(blueConfig != null)
        assert(blueConfig.get(TeamConfigType.SPAWN_STYLE) == SpawnStyle.SMALL)

        val redConfig = config.getConfigurationSection("teams.red.settings")
        require(redConfig != null)
        assert(redConfig.get(TeamConfigType.SPAWN_STYLE) == SpawnStyle.LARGE)
    }

    @Test
    fun `returns null if an invalid type is given`() {
        val configFile = File("src/test/resources/fixtures/config/predefined_items/invalid-type.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        val item = config.getConfigurationSection("invalid_item")?.toItemStack()
        require(item == null)
    }

    @Test
    fun `handles names, lore, and amount correctly`() {
        val configFile = File("src/test/resources/fixtures/config/predefined_items/sorLily.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        val item = config.getConfigurationSection("sorLily")?.toItemStack()
        require(item != null)
        assert(item.type == Material.LILY_PAD)
        assert(item.amount == 64)

        val meta = item.itemMeta
        require(meta != null)
        assert("Water Lily" in meta.displayName)

        val lore = meta.lore
        require(lore != null)
        require(lore.size == 3)
    }

    @Test
    fun `handles enchants correctly`() {
        val configFile = File("src/test/resources/fixtures/config/predefined_items/enchant-test.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        val item = config.getConfigurationSection("enchant_test")?.toItemStack()
        require(item != null)
        assert(item.type == Material.WOODEN_SWORD)
        val enchants = item.enchantments
        assert(enchants.size == 1)
    }

    @Test
    fun `handles color correctly`() {
        val configFile = File("src/test/resources/fixtures/config/predefined_items/sorLegs.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        val item = config.getConfigurationSection("sorLegs")?.toItemStack()
        require(item != null)
        assert(item.type == Material.LEATHER_LEGGINGS)

        val meta = item.itemMeta
        require(meta is LeatherArmorMeta)
        assert(meta.color == org.bukkit.Color.fromRGB(0, 255, 255))
    }

    @Test
    @Disabled("not ready yet")
    fun `handles Spigot ItemStack serialization`() {
        val configFile = File("src/test/resources/fixtures/config/predefined_items/demAxe.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        val item = config.getConfigurationSection("demAxe")?.toItemStack()
        require(item != null)
        assert(item.type == Material.STONE_AXE)

        val meta = item.itemMeta!!
        assert(meta.displayName.contains("Doom Axe"))
        assert(meta.lore!!.size == 4)
    }
}
