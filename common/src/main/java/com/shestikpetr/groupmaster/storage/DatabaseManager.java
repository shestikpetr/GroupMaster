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

    private static final int SCHEMA_VERSION = 3;

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
            migrateSchema();
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
                CREATE TABLE IF NOT EXISTS bonuses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id TEXT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
                    trigger_type TEXT NOT NULL DEFAULT 'on_join',
                    tick_interval INTEGER NOT NULL DEFAULT 0,
                    condition TEXT NOT NULL DEFAULT '{}',
                    action_type TEXT NOT NULL,
                    action_value TEXT NOT NULL,
                    merge_key TEXT NOT NULL,
                    override INTEGER NOT NULL DEFAULT 0,
                    target TEXT NOT NULL DEFAULT 'self',
                    max_stacks INTEGER NOT NULL DEFAULT 0,
                    stack_mode TEXT NOT NULL DEFAULT 'each',
                    reset_on TEXT NOT NULL DEFAULT 'never'
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_bonus_stacks (
                    player_uuid TEXT NOT NULL,
                    bonus_id INTEGER NOT NULL REFERENCES bonuses(id) ON DELETE CASCADE,
                    stacks INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (player_uuid, bonus_id)
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

    private void migrateSchema() throws SQLException {
        int currentVersion = 0;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT version FROM schema_version LIMIT 1")) {
            if (rs.next()) {
                currentVersion = rs.getInt(1);
            }
        }

        if (currentVersion < 2) {
            // v2: add target column to bonuses
            try (Statement stmt = connection.createStatement()) {
                // Check if column already exists (safe for CREATE TABLE IF NOT EXISTS)
                ResultSet rs = stmt.executeQuery("PRAGMA table_info(bonuses)");
                boolean hasTarget = false;
                while (rs.next()) {
                    if ("target".equals(rs.getString("name"))) {
                        hasTarget = true;
                        break;
                    }
                }
                if (!hasTarget) {
                    stmt.execute("ALTER TABLE bonuses ADD COLUMN target TEXT NOT NULL DEFAULT 'self'");
                    Constants.LOG.info("Database migrated to v2: added target column");
                }
            }
        }

        if (currentVersion < 3) {
            // v3: add stacking columns to bonuses + player_bonus_stacks table
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("PRAGMA table_info(bonuses)");
                boolean hasMaxStacks = false;
                while (rs.next()) {
                    if ("max_stacks".equals(rs.getString("name"))) {
                        hasMaxStacks = true;
                        break;
                    }
                }
                if (!hasMaxStacks) {
                    stmt.execute("ALTER TABLE bonuses ADD COLUMN max_stacks INTEGER NOT NULL DEFAULT 0");
                    stmt.execute("ALTER TABLE bonuses ADD COLUMN stack_mode TEXT NOT NULL DEFAULT 'each'");
                    stmt.execute("ALTER TABLE bonuses ADD COLUMN reset_on TEXT NOT NULL DEFAULT 'never'");
                    Constants.LOG.info("Database migrated to v3: added stacking columns");
                }
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS player_bonus_stacks (
                        player_uuid TEXT NOT NULL,
                        bonus_id INTEGER NOT NULL REFERENCES bonuses(id) ON DELETE CASCADE,
                        stacks INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (player_uuid, bonus_id)
                    )
                """);
            }
        }

        if (currentVersion < SCHEMA_VERSION) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("UPDATE schema_version SET version = " + SCHEMA_VERSION);
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
