package com.github.james9909.warplus.stat

import com.github.james9909.warplus.TeamKind
import com.github.james9909.warplus.WarClass
import com.github.james9909.warplus.managers.DatabaseManager
import com.github.james9909.warplus.managers.PlayerManager
import com.github.james9909.warplus.sql.models.KillModel
import com.github.james9909.warplus.sql.models.PlayerStatModel
import com.github.james9909.warplus.sql.models.WarzoneJoinLog
import com.google.common.collect.ImmutableList
import java.sql.Timestamp
import java.time.Instant
import java.util.LinkedList
import java.util.UUID

class StatTracker(private val playerManager: PlayerManager, private val databaseManager: DatabaseManager) {
    var warzoneId: Int = -1
    private val playerStats: HashMap<UUID, PlayerStatModel> = hashMapOf()
    private val killHistory = LinkedList<KillModel>()
    private val joinLog = hashMapOf<UUID, LinkedList<WarzoneJoinLog>>()

    private fun modifyPlayerStat(uuid: UUID, func: (PlayerStatModel) -> Unit) {
        playerStats[uuid] = playerStats.getOrElse(uuid, {
            PlayerStatModel.default(uuid)
        }).apply(func)
    }

    fun addKill(attacker: UUID, defender: UUID, attackerClass: WarClass, defenderClass: WarClass) {
        killHistory.add(KillModel(
            warzoneId,
            Timestamp.from(Instant.now()),
            attacker,
            defender,
            attackerClass.name,
            defenderClass.name
        ))
        modifyPlayerStat(attacker) { it.kills += 1 }
    }

    fun addDeath(victim: UUID) = modifyPlayerStat(victim) { it.deaths += 1 }

    fun addHeal(healer: UUID, amount: Int) = modifyPlayerStat(healer) { it.heals += amount }

    fun addFlagCapture(capturer: UUID) = modifyPlayerStat(capturer) { it.flagCaptures += 1 }

    fun addBomb(carrier: UUID) = modifyPlayerStat(carrier) { it.bombs += 1 }

    fun addWin(player: UUID) = modifyPlayerStat(player) { it.wins += 1 }

    fun addLoss(player: UUID) = modifyPlayerStat(player) { it.losses += 1 }

    fun addMvp(player: UUID) = modifyPlayerStat(player) { it.mvps += 1 }

    fun maxStatsBy(team: TeamKind, key: (PlayerStatModel) -> Int): Pair<UUID, Int>? {
        val max = playerStats.filter {
            playerManager.getPlayerInfo(it.key)?.team?.kind == team
        }.maxBy { entry ->
            key(entry.value)
        }
        if (max != null && key(max.value) > 0) {
            return Pair(max.key, key(max.value))
        }
        return null
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
