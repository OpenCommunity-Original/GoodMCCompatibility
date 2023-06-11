package org.opencommunity.goodmccompatibility.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.opencommunity.goodmccompatibility.functions.DatabaseManager;

public class JoinEvent implements Listener {

    private final DatabaseManager databaseManager;

    public JoinEvent(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        databaseManager.setJoinTime(player);
    }
}
