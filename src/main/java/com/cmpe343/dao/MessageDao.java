package com.cmpe343.dao;

import com.cmpe343.db.Db;
import com.cmpe343.model.Message;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MessageDao {
    
    public List<Message> getAllMessages() {
        List<Message> list = new ArrayList<>();
        // Use correct schema columns: join with users to get sender name, use text_clob, created_at, replied_at
        String sql = """
            SELECT m.id, u.username as sender, m.text_clob as content, 
                   m.created_at, (m.replied_at IS NOT NULL) as is_read
            FROM messages m
            JOIN users u ON m.customer_id = u.id
            ORDER BY m.created_at DESC
        """;
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                // Explicitly use LocalDateTime to convert timestamp
                // Add null check to prevent NullPointerException if created_at is NULL in database
                java.sql.Timestamp timestamp = rs.getTimestamp("created_at");
                LocalDateTime messageTime = timestamp != null 
                    ? timestamp.toLocalDateTime() 
                    : LocalDateTime.now(); // Fallback to current time if NULL
                list.add(new Message(
                    rs.getInt("id"),
                    rs.getString("sender"),
                    rs.getString("content"),
                    messageTime,
                    rs.getBoolean("is_read")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public List<Message> getMessagesForCustomer(int customerId) {
        List<Message> list = new ArrayList<>();
        // Use correct schema columns: join with users to get sender name, use text_clob, created_at, replied_at
        String sql = """
            SELECT m.id, u.username as sender, m.text_clob as content, 
                   m.created_at, (m.replied_at IS NOT NULL) as is_read
            FROM messages m
            JOIN users u ON m.customer_id = u.id
            WHERE m.customer_id = ? 
            ORDER BY m.created_at DESC
        """;
        
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Explicitly use LocalDateTime to convert timestamp
                    // Add null check to prevent NullPointerException if created_at is NULL in database
                    java.sql.Timestamp timestamp = rs.getTimestamp("created_at");
                    LocalDateTime messageTime = timestamp != null 
                        ? timestamp.toLocalDateTime() 
                        : LocalDateTime.now(); // Fallback to current time if NULL
                    list.add(new Message(
                        rs.getInt("id"),
                        rs.getString("sender"),
                        rs.getString("content"),
                        messageTime,
                        rs.getBoolean("is_read")
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
    
    /**
     * Formats a message timestamp for display.
     * Uses LocalDateTime to format when the message was sent.
     * 
     * @param message The message whose timestamp should be formatted
     * @return A formatted string showing when the message was sent
     */
    public String formatMessageTimestamp(Message message) {
        if (message == null || message.getTimestamp() == null) {
            return "Unknown time";
        }
        LocalDateTime timestamp = message.getTimestamp();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        return timestamp.format(formatter);
    }
    
    /**
     * Formats a LocalDateTime timestamp for display.
     * 
     * @param timestamp The LocalDateTime to format
     * @return A formatted string showing when the message was sent
     */
    public String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "Unknown time";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
        return timestamp.format(formatter);
    }
    
    public void markAsRead(int messageId) {
        // Use correct schema: set replied_at instead of is_read
        // Note: This marks the message as "replied to" rather than just "read"
        // If you need a separate read status, you'd need to add an is_read column to the schema
        String sql = "UPDATE messages SET replied_at = CURRENT_TIMESTAMP WHERE id = ? AND replied_at IS NULL";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
