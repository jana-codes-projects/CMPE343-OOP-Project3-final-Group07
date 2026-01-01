package com.cmpe343.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class SchemaValidator {

    // Table Definitions from the User's provided SQL
    private static final Map<String, String> TABLES = new HashMap<>();

    static {
        TABLES.put("users", """
                    CREATE TABLE users (
                      id            INT AUTO_INCREMENT PRIMARY KEY,
                      username      VARCHAR(50) NOT NULL UNIQUE,
                      password_hash CHAR(64)    NOT NULL,
                      role          ENUM('customer','carrier','owner') NOT NULL,
                      is_active     TINYINT(1)  NOT NULL DEFAULT 1,
                      address       VARCHAR(255),
                      phone         VARCHAR(30),
                      created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB
                """);

        TABLES.put("products", """
                    CREATE TABLE products (
                      id            INT AUTO_INCREMENT PRIMARY KEY,
                      name          VARCHAR(100) NOT NULL,
                      type          ENUM('VEG','FRUIT') NOT NULL,
                      price         DECIMAL(10,2) NOT NULL,
                      stock_kg      DECIMAL(10,2) NOT NULL,
                      threshold_kg  DECIMAL(10,2) NOT NULL,
                      image_blob    LONGBLOB COMMENT 'Product image stored as Binary Large Object (BLOB)',
                      is_active     TINYINT(1) NOT NULL DEFAULT 1,
                      created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB
                """);

        TABLES.put("coupons", """
                    CREATE TABLE coupons (
                      id         INT AUTO_INCREMENT PRIMARY KEY,
                      code       VARCHAR(30) NOT NULL UNIQUE,
                      kind       ENUM('AMOUNT','PERCENT') NOT NULL,
                      value      DECIMAL(10,2) NOT NULL,
                      min_cart   DECIMAL(10,2) NOT NULL DEFAULT 0,
                      is_active  TINYINT(1) NOT NULL DEFAULT 1,
                      expires_at DATETIME NULL
                    ) ENGINE=InnoDB
                """);

        TABLES.put("orders",
                """
                            CREATE TABLE orders (
                              id                      INT AUTO_INCREMENT PRIMARY KEY,
                              customer_id             INT NOT NULL,
                              carrier_id              INT NULL,
                              status                  ENUM('CREATED','ASSIGNED','DELIVERED','CANCELLED') NOT NULL DEFAULT 'CREATED',
                              order_time              DATETIME NOT NULL,
                              requested_delivery_time DATETIME NOT NULL,
                              delivered_time          DATETIME NULL,
                              total_before_tax        DECIMAL(10,2) NOT NULL DEFAULT 0,
                              vat                     DECIMAL(10,2) NOT NULL DEFAULT 0,
                              total_after_tax         DECIMAL(10,2) NOT NULL DEFAULT 0,
                              coupon_id               INT NULL,
                              loyalty_discount        DECIMAL(10,2) NOT NULL DEFAULT 0,
                              CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users(id),
                              CONSTRAINT fk_orders_carrier  FOREIGN KEY (carrier_id)  REFERENCES users(id),
                              CONSTRAINT fk_orders_coupon   FOREIGN KEY (coupon_id)   REFERENCES coupons(id)
                            ) ENGINE=InnoDB
                        """);

        TABLES.put("order_items", """
                    CREATE TABLE order_items (
                      id                 INT AUTO_INCREMENT PRIMARY KEY,
                      order_id           INT NOT NULL,
                      product_id         INT NOT NULL,
                      kg                 DECIMAL(10,2) NOT NULL,
                      unit_price_applied DECIMAL(10,2) NOT NULL,
                      line_total         DECIMAL(10,2) NOT NULL,
                      CONSTRAINT fk_items_order   FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                      CONSTRAINT fk_items_product FOREIGN KEY (product_id) REFERENCES products(id)
                    ) ENGINE=InnoDB
                """);

        TABLES.put("messages", """
                    CREATE TABLE messages (
                      id          INT AUTO_INCREMENT PRIMARY KEY,
                      customer_id INT NOT NULL,
                      owner_id    INT NOT NULL,
                      sender_id   INT NULL,
                      text_clob   LONGTEXT NOT NULL COMMENT 'Message text stored as Character Large Object (CLOB)',
                      created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      reply_text  LONGTEXT NULL COMMENT 'Reply text stored as Character Large Object (CLOB)',
                      replied_at  TIMESTAMP NULL,
                      CONSTRAINT fk_msg_customer FOREIGN KEY (customer_id) REFERENCES users(id),
                      CONSTRAINT fk_msg_owner    FOREIGN KEY (owner_id)    REFERENCES users(id),
                      CONSTRAINT fk_msg_sender   FOREIGN KEY (sender_id)   REFERENCES users(id)
                    ) ENGINE=InnoDB
                """);

        TABLES.put("ratings", """
                    CREATE TABLE ratings (
                      id          INT AUTO_INCREMENT PRIMARY KEY,
                      order_id    INT NOT NULL UNIQUE,
                      carrier_id  INT NOT NULL,
                      customer_id INT NOT NULL,
                      rating      TINYINT NOT NULL,
                      comment     VARCHAR(255),
                      created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      CONSTRAINT fk_rat_order    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                      CONSTRAINT fk_rat_carrier  FOREIGN KEY (carrier_id) REFERENCES users(id),
                      CONSTRAINT fk_rat_customer FOREIGN KEY (customer_id) REFERENCES users(id),
                      CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5)
                    ) ENGINE=InnoDB
                """);

        TABLES.put("invoices",
                """
                            CREATE TABLE invoices (
                              order_id      INT PRIMARY KEY,
                              pdf_blob      LONGBLOB NOT NULL COMMENT 'PDF invoice stored as Binary Large Object (BLOB)',
                              invoice_text  LONGTEXT NULL COMMENT 'Invoice/transaction log text stored as Character Large Object (CLOB)',
                              transaction_log LONGTEXT NULL COMMENT 'Transaction log details stored as Character Large Object (CLOB)',
                              created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT fk_inv_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
                            ) ENGINE=InnoDB
                        """);

        TABLES.put("cart_items", """
                    CREATE TABLE cart_items (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL,
                        product_id INT NOT NULL,
                        quantity_kg DOUBLE DEFAULT 1.0,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY unique_cart_item (user_id, product_id),
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
                    ) ENGINE=InnoDB
                """);
    }

    public static void validateAndFix() {
        System.out.println(">>> Checking Database Schema...");
        try (Connection conn = Db.getConnection();
                Statement stmt = conn.createStatement()) {

            DatabaseMetaData meta = conn.getMetaData();

            // 1. Check Tables
            for (Map.Entry<String, String> entry : TABLES.entrySet()) {
                String tableName = entry.getKey();
                String createSql = entry.getValue();

                if (!tableExists(meta, tableName)) {
                    System.out.println("   [FIX] Creating missing table: " + tableName);
                    stmt.execute(createSql);
                } else {
                    // Table exists, check specific critical columns
                    checkColumns(conn, tableName);
                }
            }

            System.out.println(">>> Database Schema Verified.");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("!!! Database Schema Validation Failed !!!");
        }
    }

    private static boolean tableExists(DatabaseMetaData meta, String tableName) throws Exception {
        try (ResultSet rs = meta.getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    private static void checkColumns(Connection conn, String tableName) throws Exception {
        Statement stmt = conn.createStatement();

        // Critical Column Checks (Add if missing)
        if (tableName.equals("products")) {
            if (!columnExists(conn, "products", "image_blob")) {
                System.out.println("   [FIX] Adding missing column: products.image_blob");
                stmt.execute(
                        "ALTER TABLE products ADD COLUMN image_blob LONGBLOB COMMENT 'Product image stored as Binary Large Object (BLOB)'");
            }
            if (!columnExists(conn, "products", "threshold_kg")) {
                System.out.println("   [FIX] Adding missing column: products.threshold_kg");
                stmt.execute("ALTER TABLE products ADD COLUMN threshold_kg DECIMAL(10,2) NOT NULL DEFAULT 0");
            }
        }

        if (tableName.equals("invoices")) {
            if (!columnExists(conn, "invoices", "pdf_blob")) {
                System.out.println("   [FIX] Adding missing column: invoices.pdf_blob");
                stmt.execute("ALTER TABLE invoices ADD COLUMN pdf_blob LONGBLOB");
            }
        }

        if (tableName.equals("messages")) {
            if (!columnExists(conn, "messages", "sender_id")) {
                System.out.println("   [FIX] Adding missing column: messages.sender_id");
                stmt.execute("ALTER TABLE messages ADD COLUMN sender_id INT NULL");
                stmt.execute(
                        "ALTER TABLE messages ADD CONSTRAINT fk_msg_sender FOREIGN KEY (sender_id) REFERENCES users(id)");
            }
        }

        // Add more specific column checks here as needed
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }
}
