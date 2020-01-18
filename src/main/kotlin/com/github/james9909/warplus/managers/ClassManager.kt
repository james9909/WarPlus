package com.github.james9909.warplus.managers

import com.github.james9909.warplus.WarClass
import com.github.james9909.warplus.WarPlus
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ClassManager(private val plugin: WarPlus) {

    private val classes: MutableMap<String, WarClass> = HashMap()

    fun loadClasses() {
        val file = File(plugin.dataFolder, "classes.yml")
        if (!file.exists()) {
            return
        }
        val config = YamlConfiguration.loadConfiguration(file)
        val classList = config.getConfigurationSection("classes") ?: return
        for (className in classList.getKeys(false)) {
            plugin.logger.info("Loading class $className")
            addClass(
                className,
                WarClass.fromConfig(className, classList.getConfigurationSection(className)!!) // NPE is impossible
            )
            plugin.logger.info("Loaded class $className")
        }
    }

    fun addClass(name: String, warClass: WarClass) {
        classes[name.toLowerCase()] = warClass
    }

    fun removeClass(name: String): Boolean {
        return classes.remove(name.toLowerCase()) != null
    }

    fun getClass(name: String): WarClass? {
        return classes[name.toLowerCase()]
    }

    fun saveConfig() {
        val file = File(plugin.dataFolder, "classes.yml")
        val config = YamlConfiguration()
        val classesSection = config.createSection("classes")
        for ((_, warClass) in classes) {
            val classSection = classesSection.createSection(warClass.name)
            warClass.saveConfig(classSection)
        }
        config.save(file)
    }

    companion object {
        private fun defaultClasses(): List<WarClass> {
            return listOf()
        }
    }
}