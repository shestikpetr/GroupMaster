package com.shestikpetr.groupmaster.storage;

import com.shestikpetr.groupmaster.model.PlayerGroupData;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlayerRepository {

    private final DatabaseManager db;

    public PlayerRepository(DatabaseManager db) {
        this.db = db;
    }

    public void assign(PlayerGroupData data) throws SQLException {
        String sql = """
            INSERT INTO player_groups (player_uuid, player_name, group_id, assigned_at, assigned_by)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET
                player_name = excluded.player_name,
                group_id = excluded.group_id,
                assigned_at = excluded.assigned_at,
                assigned_by = excluded.assigned_by
        """;
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, data.getPlayerUuid().toString());
            stmt.setString(2, data.getPlayerName());
            stmt.setString(3, data.getGroupId());
            stmt.setLong(4, data.getAssignedAt());
            stmt.setString(5, data.getAssignedBy());
            stmt.executeUpdate();
        }
    }

    public Optional<PlayerGroupData> findByUuid(UUID uuid) throws SQLException {
        String sql = "SELECT player_uuid, player_name, group_id, assigned_at, assigned_by FROM player_groups WHERE player_uuid = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public List<PlayerGroupData> findByGroup(String groupId) throws SQLException {
        List<PlayerGroupData> players = new ArrayList<>();
        String sql = "SELECT player_uuid, player_name, group_id, assigned_at, assigned_by FROM player_groups WHERE group_id = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, groupId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                players.add(mapRow(rs));
            }
        }
        return players;
    }

    public List<PlayerGroupData> findAll() throws SQLException {
        List<PlayerGroupData> players = new ArrayList<>();
        String sql = "SELECT player_uuid, player_name, group_id, assigned_at, assigned_by FROM player_groups";
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                players.add(mapRow(rs));
            }
        }
        return players;
    }

    public boolean remove(UUID uuid) throws SQLException {
        String sql = "DELETE FROM player_groups WHERE player_uuid = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            return stmt.executeUpdate() > 0;
        }
    }

    public int countByGroup(String groupId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM player_groups WHERE group_id = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, groupId);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }

    private PlayerGroupData mapRow(ResultSet rs) throws SQLException {
        return new PlayerGroupData(
            UUID.fromString(rs.getString("player_uuid")),
            rs.getString("player_name"),
            rs.getString("group_id"),
            rs.getLong("assigned_at"),
            rs.getString("assigned_by")
        );
    }
}
