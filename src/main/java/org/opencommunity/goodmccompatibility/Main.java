package org.opencommunity.goodmccompatibility;

import org.bukkit.configuration.Configuration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.opencommunity.goodmccompatibility.events.ExitEvent;
import org.opencommunity.goodmccompatibility.events.JoinEvent;
import org.opencommunity.goodmccompatibility.functions.DatabaseManager;
import org.opencommunity.goodmccompatibility.utils.SQLAPI;

import java.util.concurrent.CompletableFuture;

public class Main extends JavaPlugin implements Listener {

    private Configuration config;
    private SQLAPI api;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        // Save the default config file if it doesn't already exist and reload the latest version
        saveAndReloadConfig();
        api = new SQLAPI(config);
        // Connect to the database
        CompletableFuture<Void> connectFuture = api.connect();
        // Initialize the database manager
        databaseManager = new DatabaseManager(api);
        // Initialize and register the event listeners
        initializeEventListeners();
    }

    public void onDisable() {
        // Unregister events
        HandlerList.unregisterAll();
        // Close database
        api.close();
    }

    private void saveAndReloadConfig() {
        saveDefaultConfig();
        reloadConfig();
        config = getConfig();
    }

    private void initializeEventListeners() {
        getServer().getPluginManager().registerEvents(new JoinEvent(databaseManager), this);
        getServer().getPluginManager().registerEvents(new ExitEvent(databaseManager), this);
    }
}