package com.github.james9909.warplus.util

enum class Message(val msg: String) {
    TOO_MANY_PLAYERS("This warzone is full!"),
    NO_SUCH_WARZONE("No warzone with that name exists!"),
    WARZONE_DISABLED("This warzone is disabled!"),
    WARZONE_EDITING("This warzone is currently being edited"),
    UNLOADING_WAR_START("Unloading War..."),
    UNLOADING_WAR_DONE("Unloaded War"),
    LOADING_WAR_START("Loading War..."),
    LOADING_WAR_DONE("Loaded War"),
    ALREADY_LOADED("War is already loaded!"),
    ALREADY_UNLOADED("War is already unloaded!")
}
