package com.github.james9909.warplus.extensions

import be.seeseemelk.mockbukkit.MockBukkit
import com.github.james9909.warplus.WarPlus
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.junit.Ignore
import org.junit.jupiter.api.AfterAll
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
        MockBukkit.unload()
    }

    @Test
    fun `returns null if an invalid type is given`() {
        val configFile = File("src/test/resources/fixtures/config/predefined_items/invalid-type.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        val item = config.getConfigurationSection("invalid_item")?.toItemStack()
        require(item == null)
    }

    @Test
    fun `handles names and lore correctly`() {
        val configFile = File("src/test/resources/fixtures/config/predefined_items/sorLily.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)
        val item = config.getConfigurationSection("sorLily")?.toItemStack()
        require(item != null)
        assert(item.type == Material.LILY_PAD)

        val meta = item.itemMeta
        require(meta != null)
        assert("Water Lily" in meta.displayName)

        val lore = meta.lore
        require(lore != null)
        require(lore.size == 3)
    }

    @Test
    @Ignore("not ready yet")
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
    @Ignore("not ready yet")
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
