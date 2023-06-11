package org.opencommunity.goodmccompatibility.functions;

import org.bukkit.entity.Player;
import org.opencommunity.goodmccompatibility.utils.SQLAPI;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final Map<UUID, Long> playTimeMap;

    private final SQLAPI sqlapi;

    public DatabaseManager(SQLAPI sqlapi) {
        this.playTimeMap = new HashMap<>();
        this.sqlapi = sqlapi;
    }

    public void setJoinTime(Player player) {
        playTimeMap.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void updateTotalPlayTime(Player player) {
        if (player == null) {
            return;
        }

        UUID playerId = player.getUniqueId();

        if (!playTimeMap.containsKey(playerId)) {
            return; // Player has no join time stored, exit early
        }

        long joinTime = playTimeMap.get(playerId);
        long sessionTime = (System.currentTimeMillis() - joinTime) / (1000 * 60);

        // Retrieve the current total play time and player name from the database for the player
        CompletableFuture<PlayerData> future = sqlapi.executeQuery("SELECT total_play_time, player_name FROM player_times WHERE player_uuid = ?",
                        playerId.toString())
                .thenApply(resultSet -> {
                    try {
                        if (resultSet.next()) {
                            long totalPlayTime = resultSet.getLong("total_play_time");
                            String playerName = resultSet.getString("player_name");
                            return new PlayerData(playerName, totalPlayTime);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                });

        // Update the total play time in the database for the player
        future.thenAccept(playerData -> {
            if (playerData != null) {
                long updatedTotalPlayTime = playerData.getTotalPlayTime() + sessionTime;
                sqlapi.executeUpdate("UPDATE player_times SET total_play_time = ? WHERE player_uuid = ?",
                        updatedTotalPlayTime, playerId.toString());
            } else {
                // Player is joining for the first time, insert a new record
                sqlapi.executeUpdate("INSERT INTO player_times (player_uuid, player_name, total_play_time) VALUES (?, ?, ?)",
                        playerId.toString(), player.getName(), sessionTime);
            }
        });

        // Remove the player's join time from the RAM map
        playTimeMap.remove(playerId);
    }

    private static class PlayerData {
        private final String playerName;
        private final long totalPlayTime;

        public PlayerData(String playerName, long totalPlayTime) {
            this.playerName = playerName;
            this.totalPlayTime = totalPlayTime;
        }

        public String getPlayerName() {
            return playerName;
        }

        public long getTotalPlayTime() {
            return totalPlayTime;
        }
    }
}
