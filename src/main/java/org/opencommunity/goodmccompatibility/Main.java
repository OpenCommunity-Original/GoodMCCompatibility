package org.opencommunity.goodmoderation;

import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.opencommunity.goodmoderation.commands.ModerCommands;
import org.opencommunity.goodmoderation.config.ConfigManager;
import org.opencommunity.goodmoderation.events.PreLogin;
import org.opencommunity.goodmoderation.functions.Database;
import org.opencommunity.goodmoderation.functions.ReasonManager;
import org.opencommunity.goodmoderation.functions.Rollback;
import org.opencommunity.goodmoderation.hooks.Discord;
import org.opencommunity.goodmoderation.utils.SQLAPI;

import java.sql.Connection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class Main extends JavaPlugin implements Listener {

    private Configuration config;
    private Connection connection;
    private ConfigManager configManager;
    private Rollback rollback;
    private Database db;
    private final SQLAPI api = new SQLAPI(getDataFolder().getAbsolutePath());
    private ReasonManager reasonManager;

    @Override
    public void onEnable() {
        // Save the default config file if it doesn't already exist and reload the latest version
        saveAndReloadConfig();
        // Initialize and register the event listeners
        initializeEventListeners();
        // Initialize the database
        loadDatabase();
        // Initialize the chat command and register it with Bukkit
        initializeChatCommand();
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
        configManager = new ConfigManager(config);
        db = new Database(api, configManager);
        PreLogin preLogin = new PreLogin(db);
        Bukkit.getPluginManager().registerEvents(preLogin, this);
        Discord discord = new Discord(configManager);
        rollback = new Rollback(configManager);
        reasonManager = new ReasonManager(configManager);
    }

    private void initializeChatCommand() {
        moderCommands = new ModerCommands(db, rollback, reasonManager);
        Objects.requireNonNull(getCommand("ban")).setExecutor(moderCommands);
        Objects.requireNonNull(getCommand("unban")).setExecutor(moderCommands);
        Objects.requireNonNull(getCommand("banlist")).setExecutor(moderCommands);
    }

    private CompletableFuture<Boolean> loadDatabase() {
        return api.connect().thenCompose(aVoid -> api.executeUpdate("CREATE TABLE IF NOT EXISTS bans (uuid TEXT PRIMARY KEY, name TEXT, reason TEXT, moderator TEXT, ban_date DATE)"))
                .thenApply(result -> result > 0);
    }
}