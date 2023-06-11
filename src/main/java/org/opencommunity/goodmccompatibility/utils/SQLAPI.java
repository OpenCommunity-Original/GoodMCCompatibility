package org.opencommunity.goodmoderation.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

    public SQLAPI(String configFile) {
        Properties properties = loadConfig(configFile);

        this.host = properties.getProperty("host");
        this.port = Integer.parseInt(properties.getProperty("port"));
        this.database = properties.getProperty("database");
        this.username = properties.getProperty("username");
        this.password = properties.getProperty("password");
    }

    private Properties loadConfig(String configFile) {
        Properties properties = new Properties();
        try (FileInputStream inputStream = new FileInputStream(configFile)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config file: " + configFile, e);
        }
        return properties;
    }

    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
                Properties connectionProps = new Properties();
                connectionProps.setProperty("user", username);
                connectionProps.setProperty("password", password);
                connection = DriverManager.getConnection(url, connectionProps);
            } catch (Exception e) {
                System.err.println("Failed to connect to the database:");
                e.printStackTrace();
            }
        });
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
