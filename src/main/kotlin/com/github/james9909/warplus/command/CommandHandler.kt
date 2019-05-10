package com.github.james9909.warplus.command

class CommandHandler {

    fun getCommands(command: Array<out String>): List<AbstractCommand> {
        val command = command[0]
        val args = command.drop(1)

        return listOf()
    }
}