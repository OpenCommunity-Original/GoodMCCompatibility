package org.opencommunity.goodmccompatibility.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.opencommunity.goodmccompatibility.functions.DatabaseManager;

public class ExitEvent implements Listener {

    private final DatabaseManager databaseManager;

    public ExitEvent(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        databaseManager.updateTotalPlayTime(player);
    }
}

