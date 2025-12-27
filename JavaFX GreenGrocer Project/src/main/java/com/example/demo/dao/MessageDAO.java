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
            
            System.out.println("DEBUG MessageDAO: Executing query: " + sql + " with ownerId=" + ownerId);
            stmt.setInt(1, ownerId);
            ResultSet rs = stmt.executeQuery();
            
            int count = 0;
            while (rs.next()) {
                count++;
                int msgId = rs.getInt("id");
                int customerId = rs.getInt("customer_id");
                int msgOwnerId = rs.getInt("owner_id");
                System.out.println("DEBUG MessageDAO: Found message ID: " + msgId + ", customer_id: " + customerId + ", owner_id: " + msgOwnerId);
                messages.add(mapResultSetToMessage(rs));
            }
            System.out.println("DEBUG MessageDAO: Total messages found: " + count + ", returning list size: " + messages.size());
        } catch (SQLException e) {
            System.err.println("ERROR MessageDAO: Exception while getting messages for owner: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }
    
    /**
     * Gets all messages for all active owners.
     * 
     * @return list of messages
     */
    public List<Message> getMessagesForAllOwners() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.* FROM messages m " +
                     "INNER JOIN users u ON m.owner_id = u.id " +
                     "WHERE u.role = 'owner' AND u.is_active = 1 " +
                     "ORDER BY m.created_at DESC";
        try (Connection conn = dbAdapter.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            System.out.println("DEBUG MessageDAO: Executing query to get messages for all active owners");
            int count = 0;
            while (rs.next()) {
                count++;
                int msgId = rs.getInt("id");
                int customerId = rs.getInt("customer_id");
                int msgOwnerId = rs.getInt("owner_id");
                System.out.println("DEBUG MessageDAO: Found message ID: " + msgId + ", customer_id: " + customerId + ", owner_id: " + msgOwnerId);
                messages.add(mapResultSetToMessage(rs));
            }
            System.out.println("DEBUG MessageDAO: Total messages found for all owners: " + count + ", returning list size: " + messages.size());
        } catch (SQLException e) {
            System.err.println("ERROR MessageDAO: Exception while getting messages for all owners: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }

    /**
     * Updates a message with a reply.
     * 
     * @param messageId the message ID
     * @param reply the reply text
     * @return true if update is successful
     */
    public boolean replyToMessage(int messageId, String reply) {
        System.out.println("DEBUG MessageDAO: ========== replyToMessage CALLED ==========");
        System.out.println("DEBUG MessageDAO: Message ID: " + messageId);
        System.out.println("DEBUG MessageDAO: Reply text: " + (reply != null ? reply : "NULL"));
        
        if (reply == null) {
            System.err.println("ERROR MessageDAO: Reply text is null");
            return false;
        }
        
        String sql = "UPDATE messages SET reply_text = ?, replied_at = NOW() WHERE id = ?";
        System.out.println("DEBUG MessageDAO: SQL query: " + sql);
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            System.out.println("DEBUG MessageDAO: Getting database connection...");
            conn = dbAdapter.getConnection();
            System.out.println("DEBUG MessageDAO: Got connection: " + (conn != null ? "NOT NULL" : "NULL"));
            
            // Check if connection is valid
            if (conn == null || conn.isClosed()) {
                System.err.println("ERROR MessageDAO: Database connection is null or closed!");
                return false;
            }
            
            // Ensure auto-commit is enabled (default should be true, but just in case)
            boolean wasAutoCommit = conn.getAutoCommit();
            if (!wasAutoCommit) {
                conn.setAutoCommit(true);
                System.out.println("DEBUG MessageDAO: Auto-commit was disabled, enabled it");
            }
            
            stmt = conn.prepareStatement(sql);
            
            System.out.println("DEBUG MessageDAO: Attempting to reply to message ID: " + messageId);
            System.out.println("DEBUG MessageDAO: Connection valid: " + (!conn.isClosed()));
            System.out.println("DEBUG MessageDAO: Auto-commit: " + conn.getAutoCommit());
            System.out.println("DEBUG MessageDAO: Reply text length: " + reply.length());
            System.out.println("DEBUG MessageDAO: Reply text preview: " + (reply.length() > 50 ? reply.substring(0, 50) + "..." : reply));
            
            stmt.setString(1, reply);
            stmt.setInt(2, messageId);
            
            int rowsAffected = stmt.executeUpdate();
            System.out.println("DEBUG MessageDAO: UPDATE query executed. Rows affected: " + rowsAffected);
            
            // If auto-commit was disabled, manually commit
            if (!wasAutoCommit) {
                conn.commit();
                System.out.println("DEBUG MessageDAO: Manually committed transaction");
            }
            
            if (rowsAffected > 0) {
                System.out.println("DEBUG MessageDAO: Successfully updated reply for message ID: " + messageId);
                
                // Verify the update was successful by re-reading the message
                String verifySql = "SELECT reply_text, replied_at FROM messages WHERE id = ?";
                try (PreparedStatement verifyStmt = conn.prepareStatement(verifySql)) {
                    verifyStmt.setInt(1, messageId);
                    ResultSet rs = verifyStmt.executeQuery();
                    if (rs.next()) {
                        String savedReply = rs.getString("reply_text");
                        Timestamp repliedAt = rs.getTimestamp("replied_at");
                        System.out.println("DEBUG MessageDAO: Verified reply saved. Length: " + (savedReply != null ? savedReply.length() : 0));
                        System.out.println("DEBUG MessageDAO: Replied at: " + (repliedAt != null ? repliedAt.toString() : "null"));
                        if (savedReply != null && savedReply.equals(reply)) {
                            System.out.println("DEBUG MessageDAO: Reply text matches exactly");
                        } else {
                            System.out.println("DEBUG MessageDAO: WARNING - Reply text might not match");
                        }
                    } else {
                        System.err.println("ERROR MessageDAO: Could not verify reply - message not found after update!");
                    }
                }
                
                return true;
            } else {
                System.err.println("WARNING MessageDAO: UPDATE query returned 0 rows affected for message ID: " + messageId);
                // Check if message exists
                String checkSql = "SELECT id, customer_id, owner_id FROM messages WHERE id = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, messageId);
                    ResultSet rs = checkStmt.executeQuery();
                    if (rs.next()) {
                        System.err.println("ERROR MessageDAO: Message exists but UPDATE failed. Message ID: " + messageId + 
                                          ", Customer ID: " + rs.getInt("customer_id") + 
                                          ", Owner ID: " + rs.getInt("owner_id"));
                    } else {
                        System.err.println("ERROR MessageDAO: Message with ID " + messageId + " does not exist in database!");
                    }
                }
                return false;
            }
        } catch (SQLException e) {
            System.err.println("ERROR MessageDAO: ========== SQLException CAUGHT ==========");
            System.err.println("ERROR MessageDAO: Message ID: " + messageId);
            System.err.println("ERROR MessageDAO: Error Message: " + e.getMessage());
            System.err.println("ERROR MessageDAO: SQL State: " + e.getSQLState());
            System.err.println("ERROR MessageDAO: Error Code: " + e.getErrorCode());
            System.err.println("ERROR MessageDAO: SQLException class: " + e.getClass().getName());
            e.printStackTrace();
            System.err.println("ERROR MessageDAO: =========================================");
        } catch (Exception e) {
            System.err.println("ERROR MessageDAO: ========== Unexpected Exception CAUGHT ==========");
            System.err.println("ERROR MessageDAO: Exception type: " + e.getClass().getName());
            System.err.println("ERROR MessageDAO: Exception message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("ERROR MessageDAO: ================================================");
        } finally {
            System.out.println("DEBUG MessageDAO: In finally block");
            // Don't close connection here as it's managed by DatabaseAdapter singleton
            // Only close the statement
            if (stmt != null) {
                try {
                    System.out.println("DEBUG MessageDAO: Closing statement...");
                    stmt.close();
                    System.out.println("DEBUG MessageDAO: Statement closed");
                } catch (SQLException e) {
                    System.err.println("ERROR MessageDAO: Failed to close statement: " + e.getMessage());
                }
            } else {
                System.out.println("DEBUG MessageDAO: Statement was null, nothing to close");
            }
            System.out.println("DEBUG MessageDAO: ========== replyToMessage END ==========");
        }
        System.out.println("DEBUG MessageDAO: Returning false from replyToMessage");
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
            LocalDateTime createdDateTime = createdAt.toLocalDateTime();
            message.setCreatedAt(createdDateTime);
        }
        
        // Get reply fields if they exist
        try {
            String replyText = rs.getString("reply_text");
            if (replyText != null && !replyText.isEmpty()) {
                message.setReply(replyText);
            }
            
            Timestamp repliedAt = rs.getTimestamp("replied_at");
            if (repliedAt != null) {
                message.setRepliedAt(repliedAt.toLocalDateTime());
            }
        } catch (SQLException e) {
            // Columns might not exist in older database schemas - ignore
        }
        
        return message;
    }
}

