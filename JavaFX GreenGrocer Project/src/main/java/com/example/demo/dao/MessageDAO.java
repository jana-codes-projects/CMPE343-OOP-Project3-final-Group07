package com.example.demo.dao;

import com.example.demo.db.DatabaseAdapter;
import com.example.demo.models.Message;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Message operations.
 * Handles all database operations related to messages.
 * 
 * @author Group07
 * @version 1.0
 */
public class MessageDAO {
    private final DatabaseAdapter dbAdapter;

    /**
     * Constructor.
     */
    public MessageDAO() {
        this.dbAdapter = DatabaseAdapter.getInstance();
    }

    /**
     * Creates a new message.
     * 
     * @param message the message to create
     * @return the created message with ID, or null if creation fails
     */
    public Message createMessage(Message message) {
        String sql = "INSERT INTO messages (customer_id, owner_id, text_clob) VALUES (?, ?, ?)";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, message.getCustomerId());
            stmt.setInt(2, message.getOwnerId());
            stmt.setString(3, message.getText());
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    message.setId(rs.getInt(1));
                    return message;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets all messages for a customer.
     * 
     * @param customerId the customer ID
     * @return list of messages
     */
    public List<Message> getMessagesByCustomer(int customerId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE customer_id = ? ORDER BY created_at DESC";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    /**
     * Gets all messages for the owner.
     * 
     * @param ownerId the owner ID
     * @return list of messages
     */
    public List<Message> getMessagesForOwner(int ownerId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE owner_id = ? ORDER BY created_at DESC";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, ownerId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    /**
     * Updates a message with a reply.
     * Note: The messages table in the database doesn't have a reply column.
     * This would need to be added to the database schema or handled differently.
     * For now, this method is a placeholder.
     * 
     * @param messageId the message ID
     * @param reply the reply text
     * @return true if update is successful
     */
    public boolean replyToMessage(int messageId, String reply) {
        // Note: The database schema needs to be updated to include reply fields
        // For now, this is a placeholder
        // TODO: Add reply column to messages table
        return false;
    }

    /**
     * Maps a ResultSet row to a Message object.
     * 
     * @param rs the ResultSet
     * @return the Message object
     * @throws SQLException if a database error occurs
     */
    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getInt("id"));
        message.setCustomerId(rs.getInt("customer_id"));
        message.setOwnerId(rs.getInt("owner_id"));
        message.setText(rs.getString("text_clob"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            message.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return message;
    }
}

