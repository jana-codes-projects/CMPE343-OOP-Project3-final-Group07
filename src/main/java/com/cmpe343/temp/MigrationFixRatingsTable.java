package com.cmpe343.temp;

import com.cmpe343.db.Db;
import java.sql.Connection;
import java.sql.Statement;

public class MigrationFixRatingsTable {
    public static void main(String[] args) {
        System.out.println("Migrating database: Fixing ratings table...");

        try (Connection c = Db.getConnection();
                Statement st = c.createStatement()) {

            // Drop table if exists to ensure clean state
            st.executeUpdate("DROP TABLE IF EXISTS ratings");

            // Create table with correct schema
            String createSql = """
                        CREATE TABLE ratings (
                            id INT AUTO_INCREMENT PRIMARY KEY,
                            order_id INT NOT NULL,
                            carrier_id INT NOT NULL,
                            customer_id INT NOT NULL,
                            rating INT NOT NULL,
                            comment TEXT,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (carrier_id) REFERENCES users(id),
                            FOREIGN KEY (customer_id) REFERENCES users(id),
                            FOREIGN KEY (order_id) REFERENCES orders(id)
                        )
                    """;
            st.executeUpdate(createSql);

            System.out.println("Ratings table recreated successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
