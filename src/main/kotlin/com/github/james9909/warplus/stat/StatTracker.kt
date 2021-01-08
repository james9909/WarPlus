package com.github.james9909.warplus.stat

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarClass
import com.github.james9909.warplus.managers.DatabaseManager
import com.github.james9909.warplus.sql.models.KillModel
import com.github.james9909.warplus.sql.models.PlayerStatModel
import com.github.james9909.warplus.sql.models.WarzoneJoinLog
import com.google.common.collect.ImmutableList
import java.sql.Timestamp
import java.time.Instant
import java.util.LinkedList
import java.util.UUID

class StatTracker(private val databaseManager: DatabaseManager) {
    var warzoneId: Int = -1
    private val playerStats: HashMap<UUID, PlayerStatModel> = hashMapOf()
    private val killHistory = LinkedList<KillModel>()
    private val joinLog = hashMapOf<UUID, LinkedList<WarzoneJoinLog>>()

    fun addKill(attacker: UUID, defender: UUID, attackerClass: WarClass, defenderClass: WarClass) {
        killHistory.add(KillModel(
            warzoneId,
            Timestamp.from(Instant.now()),
            attacker,
            defender,
            attackerClass.name,
            defenderClass.name
        ))
        playerStats[attacker] = playerStats.getOrElse(attacker, {
            PlayerStatModel.default(attacker)
        }).apply {
            kills += 1
        }
    }

    fun addDeath(victim: UUID) {
        playerStats[victim] = playerStats.getOrElse(victim, {
            PlayerStatModel.default(victim)
        }).apply {
            deaths += 1
        }
    }

    fun addHeal(healer: UUID, amount: Int) {
        playerStats[healer] = playerStats.getOrElse(healer, {
            PlayerStatModel.default(healer)
        }).apply {
            heals += amount
        }
    }

    fun addFlagCapture(capturer: UUID) {
        playerStats[capturer] = playerStats.getOrElse(capturer, {
            PlayerStatModel.default(capturer)
        }).apply {
            flagCaptures += 1
        }
    }

    fun addWin(player: UUID) {
        playerStats[player] = playerStats.getOrElse(player, {
            PlayerStatModel.default(player)
        }).apply {
            wins += 1
        }
    }

    fun addLoss(player: UUID) {
        playerStats[player] = playerStats.getOrElse(player, {
            PlayerStatModel.default(player)
        }).apply {
            losses += 1
        }
    }

    fun addJoin(player: UUID, team: TeamKind) {
        if (!joinLog.containsKey(player)) {
            joinLog[player] = LinkedList()
        }
        joinLog[player]!!.add(WarzoneJoinLog(
            warzoneId,
            player,
            team,
            Timestamp.from(Instant.now()),
            null
        ))
    }

    fun addLeave(player: UUID) {
        joinLog[player]?.last?.leaveTime = Timestamp.from(Instant.now())
    }

    fun flush() {
        databaseManager.writeModels(ImmutableList.copyOf(killHistory))
        databaseManager.writeModels(ImmutableList.copyOf(playerStats.values))
        databaseManager.writeModels(ImmutableList.copyOf(joinLog.values.flatten()))
        killHistory.clear()
        playerStats.clear()
        joinLog.clear()
    }
}
