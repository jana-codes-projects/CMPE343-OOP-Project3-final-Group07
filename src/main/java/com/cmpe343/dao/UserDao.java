package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDao {

    public User login(String username, String password) {
        String sql = """
            SELECT id, username, role
            FROM users
            WHERE username = ?
              AND password_hash = SHA2(?, 256)
              AND is_active = 1
        """;

        try (Connection c = Db.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);

                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role")
                    );
                }
                return null;
        } catch (Exception e) {
            throw new RuntimeException("Login Error", e);
    }
    }
}