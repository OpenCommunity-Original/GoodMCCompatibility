package org.opencommunity.goodmccompatibility.utils;

import org.bukkit.configuration.Configuration;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class SQLAPI {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private Connection connection;

    public SQLAPI(Configuration config) {
        this.host = config.getString("host");
        this.port = config.getInt("port");
        this.database = config.getString("database");
        this.username = config.getString("username");
        this.password = config.getString("password");
    }

    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
                Properties connectionProps = new Properties();
                connectionProps.setProperty("user", username);
                connectionProps.setProperty("password", password);
                connection = DriverManager.getConnection(url, connectionProps);

                // Create the table if it doesn't exist
                createTableIfNotExists();
            } catch (Exception e) {
                System.err.println("Failed to connect to the database:");
                e.printStackTrace();
            }
        });
    }

    private void createTableIfNotExists() {
        try (Statement statement = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS player_times ("
                    + "id INT PRIMARY KEY AUTO_INCREMENT,"
                    + "player_uuid VARCHAR(36) NOT NULL,"
                    + "player_name VARCHAR(16) NOT NULL,"
                    + "total_play_time BIGINT NOT NULL"
                    + ")";
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Failed to create table player_times:");
            e.printStackTrace();
        }
    }

    public CompletableFuture<Void> close() {
        return CompletableFuture.runAsync(() -> {
            try {
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<ResultSet> executeQuery(String query, Object... parameters) {
        return CompletableFuture.supplyAsync(() -> {
            ResultSet resultSet = null;
            try {
                PreparedStatement stmt = connection.prepareStatement(query);
                for (int i = 0; i < parameters.length; i++) {
                    stmt.setObject(i+1, parameters[i]);
                }
                resultSet = stmt.executeQuery();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return resultSet;
        });
    }

    public CompletableFuture<Integer> executeUpdate(String query, Object... parameters) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (int i = 0; i < parameters.length; i++) {
                    statement.setObject(i + 1, parameters[i]);
                }
                return statement.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

    public CompletableFuture<Boolean> hasResult(String query, String... params) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Thread thread = new Thread(() -> {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (int i = 0; i < params.length; i++) {
                    statement.setString(i + 1, params[i]);
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    boolean match = resultSet.next() && resultSet.getInt(1) > 0;
                    future.complete(match);
                }
            } catch (SQLException e) {
                future.completeExceptionally(e);
            }
        });
        thread.start();
        return future;
    }

    public CompletableFuture<List<Map<String, Object>>> search(String table, String searchValue) {
        return CompletableFuture.supplyAsync(() -> {
            List<Map<String, Object>> results = new ArrayList<>();
            String query = "SELECT * FROM " + table + " WHERE ";

            // Build the WHERE clause to search for the given string in any column
            List<String> columns = null;
            try {
                columns = getColumnNames(table);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            for (int i = 0; i < columns.size(); i++) {
                String column = columns.get(i);
                if (i > 0) {
                    query += " OR ";
                }
                query += column + " LIKE ?";
            }

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                // Set the search value for each parameter
                for (int i = 0; i < columns.size(); i++) {
                    statement.setString(i + 1, "%" + searchValue + "%");
                }

                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (String column : columns) {
                        row.put(column, resultSet.getObject(column));
                    }
                    results.add(row);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            return results;
        });
    }

    private List<String> getColumnNames(String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (ResultSet resultSet = connection.getMetaData().getColumns(null, null, table, null)) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }
}
