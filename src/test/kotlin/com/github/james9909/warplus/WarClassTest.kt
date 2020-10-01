package com.github.james9909.warplus

import be.seeseemelk.mockbukkit.MockBukkit
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.enchantments.Enchantment
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WarClassTest {
    private val server = MockBukkit.mock()
    private val plugin = MockBukkit.load(WarPlus::class.java)

    @AfterAll
    internal fun afterAll() {
        MockBukkit.unmock()
    }

    @Test
    fun `loads from config correctly`() {
        val configFile = File("src/test/resources/fixtures/config/classes/ranger.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)

        val ranger = WarClass.fromConfig("ranger", config.getConfigurationSection("ranger")!!)
        assert(ranger.items.size == 2)
        val bow = ranger.items[0]
        require(bow != null)
        assert(bow.type == Material.BOW)

        val arrows = ranger.items[1]
        require(arrows != null)
        assert(arrows.type == Material.ARROW)
        assert(arrows.amount == 64)

        val chest = ranger.armor.chestplate
        require(chest != null)
        assert(chest.type == Material.CHAINMAIL_CHESTPLATE)
        Enchantment.ARROW_DAMAGE
    }
}
