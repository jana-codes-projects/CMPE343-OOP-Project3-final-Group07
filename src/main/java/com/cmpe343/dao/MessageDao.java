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
    
    public static class Conversation {
        private int userId;
        private String username;
        private String lastMessage;
        private LocalDateTime lastTimestamp;
        private boolean hasUnread;

        public Conversation(int userId, String username, String lastMessage, LocalDateTime lastTimestamp,
                boolean hasUnread) {
            this.userId = userId;
            this.username = username;
            this.lastMessage = lastMessage;
            this.lastTimestamp = lastTimestamp;
            this.hasUnread = hasUnread;
        }

        public int getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getLastMessage() {
            return lastMessage;
        }

        public LocalDateTime getLastTimestamp() {
            return lastTimestamp;
        }

        public boolean hasUnread() {
            return hasUnread;
        }
    }
    
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
    
    /**
     * Creates a new message.
     * 
     * @param customerId The ID of the customer (or carrier)
     * @param ownerId    The ID of the owner
     * @param senderId   The ID of the actual sender (customer/carrier or owner)
     * @param text       The message text
     * @return The created message ID, or -1 if creation fails
     */
    public int createMessage(int customerId, int ownerId, int senderId, String text) {
        // Note: sender_id column doesn't exist in schema
        // Schema only supports: customer messages (text_clob) and owner replies (reply_text)
        // If senderId == ownerId, we need to use reply_text on existing message
        // For now, only support customer messages (senderId == customerId)
        // Owner messages should use replyToMessage() instead
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
        String sql = "UPDATE messages SET reply_text = ?, replied_at = CURRENT_TIMESTAMP WHERE id = ?";
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
                   m.created_at, (m.replied_at IS NOT NULL) as is_read,
                   m.reply_text, m.replied_at
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
     * Gets a single message by ID.
     * 
     * @param messageId The ID of the message
     * @return The message, or null if not found
     */
    public Message getMessageById(int messageId) {
        String sql = """
            SELECT m.id, u.username as sender, m.text_clob as content, 
                   m.created_at, (m.replied_at IS NOT NULL) as is_read
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
                        rs.getBoolean("is_read")
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Gets a list of conversations for the owner (unique users who have messaged).
     * 
     * @param ownerId The owner's ID
     * @return List of conversation summaries
     */
    public List<Conversation> getConversationsForOwner(int ownerId) {
        List<Conversation> list = new ArrayList<>();
        // Group by customer_id to get unique conversations
        // We want the latest message/status for each customer
        String sql = """
                    SELECT m.customer_id, u.username,
                           MAX(m.created_at) as last_time,
                           (SELECT text_clob FROM messages m2 WHERE m2.customer_id = m.customer_id AND m2.owner_id = ? ORDER BY m2.created_at DESC LIMIT 1) as last_msg,
                           SUM(CASE WHEN m.replied_at IS NULL THEN 1 ELSE 0 END) as unread_count
                    FROM messages m
                    JOIN users u ON m.customer_id = u.id
                    WHERE m.owner_id = ?
                    GROUP BY m.customer_id, u.username
                    ORDER BY last_time DESC
                """;

        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ownerId); // For subquery
            ps.setInt(2, ownerId); // For WHERE clause

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp timestamp = rs.getTimestamp("last_time");
                    LocalDateTime time = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();
                    list.add(new Conversation(
                            rs.getInt("customer_id"),
                            rs.getString("username"),
                            rs.getString("last_msg"),
                            time,
                            rs.getInt("unread_count") > 0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Gets the full chat history between an owner and a specific user.
     * 
     * @param ownerId     The ID of the owner
     * @param otherUserId The ID of the other user (customer/carrier)
     * @return List of messages in chronological order
     */
    public List<Message> getMessagesBetween(int ownerId, int otherUserId) {
        List<Message> list = new ArrayList<>();
        String sql = """
                    SELECT m.id, u.username as sender, m.text_clob as content,
                           m.created_at, (m.replied_at IS NOT NULL) as is_read,
                           m.reply_text, m.replied_at, m.customer_id
                    FROM messages m
                    JOIN users u ON m.customer_id = u.id
                    WHERE m.customer_id = ? AND m.owner_id = ?
                    ORDER BY m.created_at ASC
                """;

        try (Connection c = Db.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, otherUserId);
            ps.setInt(2, ownerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp timestamp = rs.getTimestamp("created_at");
                    LocalDateTime messageTime = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();

                    // Customer message (since sender_id doesn't exist, all rows are customer messages)
                    int senderId = rs.getInt("customer_id");
                    String senderName = rs.getString("sender");

                    Message msg = new Message(
                            rs.getInt("id"),
                            senderId,
                            senderName,
                            rs.getString("content"),
                            messageTime,
                            rs.getBoolean("is_read"));

                    list.add(msg);

                    // Handle owner reply (reply_text exists, add it as a separate message)
                    String reply = rs.getString("reply_text");
                    if (reply != null && !reply.isEmpty()) {
                        java.sql.Timestamp repliedAt = rs.getTimestamp("replied_at");
                        LocalDateTime replyTime = repliedAt != null ? repliedAt.toLocalDateTime() : messageTime.plusMinutes(1);
                        // Use a unique positive ID for replies: messageId + large offset
                        // This avoids negative IDs while maintaining uniqueness
                        int messageId = rs.getInt("id");
                        int replyId = messageId + 100000000; // Large offset to ensure uniqueness
                        Message replyMsg = new Message(
                                replyId,
                                ownerId,
                                "Owner",
                                reply,
                                replyTime,
                                true);
                        list.add(replyMsg);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
