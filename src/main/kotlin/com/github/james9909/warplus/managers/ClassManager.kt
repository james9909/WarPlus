package com.github.james9909.warplus.managers

import com.github.james9909.warplus.WarClass
import com.github.james9909.warplus.WarPlus
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ClassManager(private val plugin: WarPlus) {

    private val classes: MutableMap<String, WarClass> = LinkedHashMap()

    fun loadClasses(config: YamlConfiguration) {
        classes.clear()
        val classList = config.getConfigurationSection("classes") ?: return
        classList.getKeys(false).forEach { className ->
            plugin.logger.info("Loading class $className")
            addClass(
                className,
                WarClass.fromConfig(className, classList.getConfigurationSection(className)!!) // NPE is impossible
            )
            plugin.logger.info("Loaded class $className")
        }
    }

    fun loadClasses() {
        val file = File(plugin.dataFolder, "classes.yml")
        if (!file.exists()) plugin.saveResource("classes.yml", true)
        val config = YamlConfiguration.loadConfiguration(file)
        loadClasses(config)
    }

    fun addClass(name: String, warClass: WarClass) {
        classes[name.lowercase()] = warClass
    }

    fun removeClass(name: String): Boolean {
        return classes.remove(name.lowercase()) != null
    }

    fun getClass(name: String): WarClass? {
        return classes[name.lowercase()]
    }

    fun containsClass(name: String): Boolean {
        return classes.containsKey(name.lowercase())
    }

    fun getClassNames(): List<String> {
        return classes.keys.toList()
    }

    fun saveConfig() {
        val file = File(plugin.dataFolder, "classes.yml")
        val config = YamlConfiguration()
        val classesSection = config.createSection("classes")
        classes.values.forEach {
            val classSection = classesSection.createSection(it.name)
            it.saveConfig(classSection)
        }
        config.save(file)
    }

    fun resolveClasses(): List<String> {
        val defaultClasses = plugin.config.getStringList("classes")
        if (defaultClasses.isNotEmpty()) {
            return defaultClasses
        }
        return getClassNames()
    }
}
