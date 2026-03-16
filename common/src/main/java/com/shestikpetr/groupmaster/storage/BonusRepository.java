package com.shestikpetr.groupmaster.storage;

import com.shestikpetr.groupmaster.model.Bonus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BonusRepository {

    private final DatabaseManager db;

    public BonusRepository(DatabaseManager db) {
        this.db = db;
    }

    public Bonus create(Bonus bonus) throws SQLException {
        String sql = """
            INSERT INTO bonuses (group_id, trigger_type, tick_interval, condition, action_type, action_value, merge_key, override)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, bonus.getGroupId());
            stmt.setString(2, bonus.getTrigger());
            stmt.setInt(3, bonus.getTickInterval());
            stmt.setString(4, bonus.getCondition());
            stmt.setString(5, bonus.getActionType());
            stmt.setString(6, bonus.getActionValue());
            stmt.setString(7, bonus.getMergeKey());
            stmt.setBoolean(8, bonus.isOverride());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    bonus.setId(keys.getInt(1));
                }
            }
        }
        return bonus;
    }

    public List<Bonus> findByGroup(String groupId) throws SQLException {
        List<Bonus> bonuses = new ArrayList<>();
        String sql = "SELECT * FROM bonuses WHERE group_id = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, groupId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bonuses.add(mapRow(rs));
                }
            }
        }
        return bonuses;
    }

    public List<Bonus> findAll() throws SQLException {
        List<Bonus> bonuses = new ArrayList<>();
        String sql = "SELECT * FROM bonuses ORDER BY group_id";
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                bonuses.add(mapRow(rs));
            }
        }
        return bonuses;
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM bonuses WHERE id = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteByGroup(String groupId) throws SQLException {
        String sql = "DELETE FROM bonuses WHERE group_id = ?";
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, groupId);
            return stmt.executeUpdate() > 0;
        }
    }

    private Bonus mapRow(ResultSet rs) throws SQLException {
        return new Bonus(
                rs.getInt("id"),
                rs.getString("group_id"),
                rs.getString("trigger_type"),
                rs.getInt("tick_interval"),
                rs.getString("condition"),
                rs.getString("action_type"),
                rs.getString("action_value"),
                rs.getString("merge_key"),
                rs.getBoolean("override")
        );
    }
}
