package com.shestikpetr.groupmaster.storage;

import com.shestikpetr.groupmaster.Constants;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final int SCHEMA_VERSION = 1;

    private final Path dbPath;
    private Connection connection;

    public DatabaseManager(Path serverDir) {
        this.dbPath = serverDir.resolve("config").resolve("groupmaster").resolve("groupmaster.db");
    }

    public void init() {
        try {
            Files.createDirectories(dbPath.getParent());
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA foreign_keys=ON");
            }

            createSchema();
            Constants.LOG.info("Database initialized at {}", dbPath);
        } catch (Exception e) {
            Constants.LOG.error("Failed to initialize database", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void createSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS groups (
                    id TEXT PRIMARY KEY,
                    display_name TEXT NOT NULL,
                    parent_id TEXT REFERENCES groups(id) ON DELETE SET NULL,
                    priority INTEGER NOT NULL DEFAULT 0
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_groups (
                    player_uuid TEXT PRIMARY KEY,
                    player_name TEXT NOT NULL,
                    group_id TEXT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
                    assigned_at INTEGER NOT NULL,
                    assigned_by TEXT NOT NULL DEFAULT 'system'
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INTEGER NOT NULL
                )
            """);
        }

        boolean needsInsert;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM schema_version")) {
            needsInsert = rs.next() && rs.getInt(1) == 0;
        }

        if (needsInsert) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("INSERT INTO schema_version (version) VALUES (" + SCHEMA_VERSION + ")");
            }
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
                Constants.LOG.info("Database connection closed");
            } catch (SQLException e) {
                Constants.LOG.error("Failed to close database connection", e);
            }
        }
    }
}
