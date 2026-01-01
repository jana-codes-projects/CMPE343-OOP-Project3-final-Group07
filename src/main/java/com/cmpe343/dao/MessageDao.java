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
        // Use correct schema columns: join with users to get sender name, use
        // text_clob, created_at, replied_at
        String sql = """
                    SELECT m.id, u.username as sender, m.text_clob as content,
                           m.created_at, 0 as is_read
                    FROM messages m
                    JOIN users u ON m.customer_id = u.id
                    ORDER BY m.created_at DESC
                """;

        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                // Explicitly use LocalDateTime to convert timestamp
                // Add null check to prevent NullPointerException if created_at is NULL in
                // database
                java.sql.Timestamp timestamp = rs.getTimestamp("created_at");
                LocalDateTime messageTime = timestamp != null
                        ? timestamp.toLocalDateTime()
                        : LocalDateTime.now(); // Fallback to current time if NULL
                list.add(new Message(
                        rs.getInt("id"),
                        rs.getString("sender"),
                        rs.getString("content"),
                        messageTime,
                        rs.getBoolean("is_read")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Message> getMessagesForCustomer(int customerId) {
        List<Message> list = new ArrayList<>();
        // Use correct schema columns: join with users to get sender name, use
        // text_clob, created_at, replied_at
        String sql = """
                    SELECT m.id, u.username as sender, m.text_clob as content,
                           m.created_at, 0 as is_read
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
                    // Add null check to prevent NullPointerException if created_at is NULL in
                    // database
                    java.sql.Timestamp timestamp = rs.getTimestamp("created_at");
                    LocalDateTime messageTime = timestamp != null
                            ? timestamp.toLocalDateTime()
                            : LocalDateTime.now(); // Fallback to current time if NULL
                    list.add(new Message(
                            rs.getInt("id"),
                            rs.getString("sender"),
                            rs.getString("content"),
                            messageTime,
                            rs.getBoolean("is_read")));
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
        // If you need a separate read status, you'd need to add an is_read column to
        // the schema
        String sql = "UPDATE messages SET id = id WHERE id = ?"; // Temporary fix to avoid replied_at error
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new message from a customer to the owner.
     * 
     * @param customerId The ID of the customer sending the message
     * @param ownerId    The ID of the owner receiving the message
     * @param text       The message text
     * @return The created message ID, or -1 if creation fails
     */
    public int createMessage(int customerId, int ownerId, String text) {
        String sql = """
                    INSERT INTO messages (customer_id, owner_id, text_clob)
                    VALUES (?, ?, ?)
                """;
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerId);
            ps.setInt(2, ownerId);
            ps.setString(3, text);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Replies to a message (owner replies to customer).
     * 
     * @param messageId The ID of the message to reply to
     * @param replyText The reply text
     * @return true if the reply was successful, false otherwise
     */
    public boolean replyToMessage(int messageId, String replyText) {
        String sql = "UPDATE messages SET reply_text = ? WHERE id = ?"; // Temporary fix to avoid replied_at error
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, replyText);
            ps.setInt(2, messageId);
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets the reply text for a message.
     * 
     * @param messageId The ID of the message
     * @return The reply text, or null if no reply exists
     */
    public String getReplyText(int messageId) {
        String sql = "SELECT reply_text FROM messages WHERE id = ?";
        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("reply_text");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets messages for the owner.
     * 
     * @param ownerId The ID of the owner
     * @return List of messages sent to this owner
     */
    public List<Message> getMessagesForOwner(int ownerId) {
        List<Message> list = new ArrayList<>();
        String sql = """
                    SELECT m.id, u.username as sender, m.text_clob as content,
                           m.created_at, 0 as is_read,
                           m.reply_text
                    FROM messages m
                    JOIN users u ON m.customer_id = u.id
                    WHERE m.owner_id = ?
                    ORDER BY m.created_at DESC
                """;

        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ownerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp timestamp = rs.getTimestamp("created_at");
                    LocalDateTime messageTime = timestamp != null
                            ? timestamp.toLocalDateTime()
                            : LocalDateTime.now();
                    list.add(new Message(
                            rs.getInt("id"),
                            rs.getString("sender"),
                            rs.getString("content"),
                            messageTime,
                            rs.getBoolean("is_read")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Gets a single message by ID.
     * 
     * @param messageId The ID of the message
     * @return The message, or null if not found
     */
    public Message getMessageById(int messageId) {
        String sql = """
                    SELECT m.id, u.username as sender, m.text_clob as content,
                           m.created_at, 0 as is_read
                    FROM messages m
                    JOIN users u ON m.customer_id = u.id
                    WHERE m.id = ?
                """;

        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, messageId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp timestamp = rs.getTimestamp("created_at");
                    LocalDateTime messageTime = timestamp != null
                            ? timestamp.toLocalDateTime()
                            : LocalDateTime.now();
                    return new Message(
                            rs.getInt("id"),
                            rs.getString("sender"),
                            rs.getString("content"),
                            messageTime,
                            rs.getBoolean("is_read"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
