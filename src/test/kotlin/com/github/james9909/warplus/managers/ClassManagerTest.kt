package com.github.james9909.warplus.managers

import be.seeseemelk.mockbukkit.MockBukkit
import com.github.james9909.warplus.ArmorSet
import com.github.james9909.warplus.WarPlus
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClassManagerTest {
    private val server = MockBukkit.mock()
    private val plugin = MockBukkit.load(WarPlus::class.java)

    private val classManager = ClassManager(plugin)

    init {
        server.addSimpleWorld("flat")
    }

    @AfterAll
    internal fun afterAll() {
        MockBukkit.unmock()
    }

    @Test
    fun `loads classes properly`() {
        val configFile = File("src/test/resources/fixtures/config/classes.yml")
        val config = YamlConfiguration.loadConfiguration(configFile)

        classManager.loadClasses(config)
        assert(classManager.getClassNames() == listOf("class1", "ranger", "tank"))
        assert(classManager.containsClass("class1"))
        assert(classManager.containsClass("Class1"))
        assert(classManager.containsClass("ranger"))
        assert(classManager.containsClass("Ranger"))
        assert(classManager.containsClass("tank"))
        assert(classManager.containsClass("Tank"))
        assert(!classManager.containsClass("this_doesnt_exist"))

        classManager.getClass("class1")?.let {
            assert(it.armor == ArmorSet.default())
            assert(it.classchest == Location(server.getWorld("flat"), 70.0, 4.0, -82.0, 0F, 0F))
            assert(it.items.isEmpty())
        }
        classManager.getClass("ranger")?.let {
            assert(it.armor == ArmorSet(null, ItemStack(Material.CHAINMAIL_CHESTPLATE), null, null))
            assert(it.classchest == null)
            assert(it.items == hashMapOf(
                0 to ItemStack(Material.BOW),
                1 to ItemStack(Material.ARROW, 64)
            ))
        }
        classManager.getClass("tank")?.let {
            assert(it.armor == ArmorSet(
                null,
                ItemStack(Material.DIAMOND_CHESTPLATE),
                ItemStack(Material.DIAMOND_LEGGINGS),
                ItemStack(Material.DIAMOND_BOOTS)
            ))
            assert(it.classchest == null)
            assert(it.items == hashMapOf(
                0 to ItemStack(Material.WOODEN_SWORD)
            ))
        }
    }
}
