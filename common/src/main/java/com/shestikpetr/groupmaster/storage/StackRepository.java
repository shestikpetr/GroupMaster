package com.shestikpetr.groupmaster.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class StackRepository {

    private final DatabaseManager db;

    public StackRepository(DatabaseManager db) {
        this.db = db;
    }

    public int getStacks(String playerUuid, int bonusId) throws SQLException {
        String sql = "SELECT stacks FROM player_bonus_stacks WHERE player_uuid = ? AND bonus_id = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, bonusId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("stacks") : 0;
            }
        }
    }

    public void setStacks(String playerUuid, int bonusId, int stacks) throws SQLException {
        String sql = "INSERT INTO player_bonus_stacks (player_uuid, bonus_id, stacks) VALUES (?, ?, ?) " +
                     "ON CONFLICT(player_uuid, bonus_id) DO UPDATE SET stacks = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, bonusId);
            stmt.setInt(3, stacks);
            stmt.setInt(4, stacks);
            stmt.executeUpdate();
        }
    }

    public int incrementAndGet(String playerUuid, int bonusId) throws SQLException {
        String sql = "INSERT INTO player_bonus_stacks (player_uuid, bonus_id, stacks) VALUES (?, ?, 1) " +
                     "ON CONFLICT(player_uuid, bonus_id) DO UPDATE SET stacks = stacks + 1";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, bonusId);
            stmt.executeUpdate();
        }
        return getStacks(playerUuid, bonusId);
    }

    public void resetByPlayerAndResetType(String playerUuid, String resetOn) throws SQLException {
        String sql = "DELETE FROM player_bonus_stacks WHERE player_uuid = ? AND bonus_id IN " +
                     "(SELECT id FROM bonuses WHERE reset_on = ?)";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setString(2, resetOn);
            stmt.executeUpdate();
        }
    }

    public void resetByPlayerAndBonus(String playerUuid, int bonusId) throws SQLException {
        String sql = "DELETE FROM player_bonus_stacks WHERE player_uuid = ? AND bonus_id = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.setInt(2, bonusId);
            stmt.executeUpdate();
        }
    }

    public record StackEntry(String playerUuid, int bonusId, int stacks) {}

    public List<StackEntry> findAll() throws SQLException {
        List<StackEntry> result = new ArrayList<>();
        String sql = "SELECT player_uuid, bonus_id, stacks FROM player_bonus_stacks WHERE stacks > 0";
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new StackEntry(rs.getString("player_uuid"), rs.getInt("bonus_id"), rs.getInt("stacks")));
            }
        }
        return result;
    }

    public List<StackEntry> findByPlayer(String playerUuid) throws SQLException {
        List<StackEntry> result = new ArrayList<>();
        String sql = "SELECT player_uuid, bonus_id, stacks FROM player_bonus_stacks WHERE player_uuid = ? AND stacks > 0";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new StackEntry(rs.getString("player_uuid"), rs.getInt("bonus_id"), rs.getInt("stacks")));
                }
            }
        }
        return result;
    }

    public void resetAllForPlayer(String playerUuid) throws SQLException {
        String sql = "DELETE FROM player_bonus_stacks WHERE player_uuid = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, playerUuid);
            stmt.executeUpdate();
        }
    }
}
