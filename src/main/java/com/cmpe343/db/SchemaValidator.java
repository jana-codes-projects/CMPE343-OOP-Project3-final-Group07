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
        // generic check for all tables
        // USERS
        if (tableName.equals("users")) {
            ensureColumn(conn, stmt, "users", "username", "VARCHAR(50) NOT NULL UNIQUE");
            ensureColumn(conn, stmt, "users", "password_hash", "CHAR(64) NOT NULL");
            ensureColumn(conn, stmt, "users", "role", "ENUM('customer','carrier','owner') NOT NULL");
            ensureColumn(conn, stmt, "users", "is_active", "TINYINT(1) NOT NULL DEFAULT 1");
            ensureColumn(conn, stmt, "users", "address", "VARCHAR(255)");
            ensureColumn(conn, stmt, "users", "phone", "VARCHAR(30)");
            ensureColumn(conn, stmt, "users", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        }
        // PRODUCTS
        if (tableName.equals("products")) {
            ensureColumn(conn, stmt, "products", "name", "VARCHAR(100) NOT NULL");
            ensureColumn(conn, stmt, "products", "type", "ENUM('VEG','FRUIT') NOT NULL");
            ensureColumn(conn, stmt, "products", "price", "DECIMAL(10,2) NOT NULL");
            ensureColumn(conn, stmt, "products", "stock_kg", "DECIMAL(10,2) NOT NULL");
            ensureColumn(conn, stmt, "products", "threshold_kg", "DECIMAL(10,2) NOT NULL");
            ensureColumn(conn, stmt, "products", "image_blob",
                    "LONGBLOB COMMENT 'Product image stored as Binary Large Object (BLOB)'");
            ensureColumn(conn, stmt, "products", "is_active", "TINYINT(1) NOT NULL DEFAULT 1");
            ensureColumn(conn, stmt, "products", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        }

        // COUPONS
        if (tableName.equals("coupons")) {
            ensureColumn(conn, stmt, "coupons", "code", "VARCHAR(30) NOT NULL UNIQUE");
            ensureColumn(conn, stmt, "coupons", "kind", "ENUM('AMOUNT','PERCENT') NOT NULL");
            ensureColumn(conn, stmt, "coupons", "value", "DECIMAL(10,2) NOT NULL");
            ensureColumn(conn, stmt, "coupons", "min_cart", "DECIMAL(10,2) NOT NULL DEFAULT 0");
            ensureColumn(conn, stmt, "coupons", "is_active", "TINYINT(1) NOT NULL DEFAULT 1");
            ensureColumn(conn, stmt, "coupons", "expires_at", "DATETIME NULL");
        }

        // ORDERS
        if (tableName.equals("orders")) {
            ensureColumn(conn, stmt, "orders", "customer_id", "INT NOT NULL");
            // note: FK constraints are generally part of create table, adding them via
            // generic check only if column missing
            // we won't strictly enforce FK reconstruction here to avoid duplication errors
            // without more complex checks
            ensureColumn(conn, stmt, "orders", "carrier_id", "INT NULL");
            ensureColumn(conn, stmt, "orders", "status",
                    "ENUM('CREATED','ASSIGNED','DELIVERED','CANCELLED') NOT NULL DEFAULT 'CREATED'");
            ensureColumn(conn, stmt, "orders", "order_time", "DATETIME NOT NULL");
            ensureColumn(conn, stmt, "orders", "requested_delivery_time", "DATETIME NOT NULL");
            ensureColumn(conn, stmt, "orders", "delivered_time", "DATETIME NULL");
            ensureColumn(conn, stmt, "orders", "total_before_tax", "DECIMAL(10,2) NOT NULL DEFAULT 0");
            ensureColumn(conn, stmt, "orders", "vat", "DECIMAL(10,2) NOT NULL DEFAULT 0");
            ensureColumn(conn, stmt, "orders", "total_after_tax", "DECIMAL(10,2) NOT NULL DEFAULT 0");
            ensureColumn(conn, stmt, "orders", "coupon_id", "INT NULL");
            ensureColumn(conn, stmt, "orders", "loyalty_discount", "DECIMAL(10,2) NOT NULL DEFAULT 0");
        }

        // ORDER_ITEMS
        if (tableName.equals("order_items")) {
            ensureColumn(conn, stmt, "order_items", "order_id", "INT NOT NULL");
            ensureColumn(conn, stmt, "order_items", "product_id", "INT NOT NULL");
            ensureColumn(conn, stmt, "order_items", "kg", "DECIMAL(10,2) NOT NULL");
            ensureColumn(conn, stmt, "order_items", "unit_price_applied", "DECIMAL(10,2) NOT NULL");
            ensureColumn(conn, stmt, "order_items", "line_total", "DECIMAL(10,2) NOT NULL");
        }

        // MESSAGES
        if (tableName.equals("messages")) {
            ensureColumn(conn, stmt, "messages", "customer_id", "INT NOT NULL");
            ensureColumn(conn, stmt, "messages", "owner_id", "INT NOT NULL");
            ensureColumn(conn, stmt, "messages", "sender_id", "INT NULL");
            ensureColumn(conn, stmt, "messages", "text_clob",
                    "LONGTEXT NOT NULL COMMENT 'Message text stored as Character Large Object (CLOB)'");
            ensureColumn(conn, stmt, "messages", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
            ensureColumn(conn, stmt, "messages", "reply_text",
                    "LONGTEXT NULL COMMENT 'Reply text stored as Character Large Object (CLOB)'");
            ensureColumn(conn, stmt, "messages", "replied_at", "TIMESTAMP NULL");
        }

        // RATINGS
        if (tableName.equals("ratings")) {
            ensureColumn(conn, stmt, "ratings", "order_id", "INT NOT NULL UNIQUE");
            ensureColumn(conn, stmt, "ratings", "carrier_id", "INT NOT NULL");
            ensureColumn(conn, stmt, "ratings", "customer_id", "INT NOT NULL");
            ensureColumn(conn, stmt, "ratings", "rating", "TINYINT NOT NULL");
            ensureColumn(conn, stmt, "ratings", "comment", "VARCHAR(255)");
            ensureColumn(conn, stmt, "ratings", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        }

        // INVOICES
        if (tableName.equals("invoices")) {
            ensureColumn(conn, stmt, "invoices", "order_id", "INT PRIMARY KEY");
            ensureColumn(conn, stmt, "invoices", "pdf_blob",
                    "LONGBLOB NOT NULL COMMENT 'PDF invoice stored as Binary Large Object (BLOB)'");
            ensureColumn(conn, stmt, "invoices", "invoice_text",
                    "LONGTEXT NULL COMMENT 'Invoice/transaction log text stored as Character Large Object (CLOB)'");
            ensureColumn(conn, stmt, "invoices", "transaction_log",
                    "LONGTEXT NULL COMMENT 'Transaction log details stored as Character Large Object (CLOB)'");
            ensureColumn(conn, stmt, "invoices", "created_at", "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        }

        // CART_ITEMS
        if (tableName.equals("cart_items")) {
            ensureColumn(conn, stmt, "cart_items", "user_id", "INT NOT NULL");
            ensureColumn(conn, stmt, "cart_items", "product_id", "INT NOT NULL");
            ensureColumn(conn, stmt, "cart_items", "quantity_kg", "DOUBLE DEFAULT 1.0");
            ensureColumn(conn, stmt, "cart_items", "created_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        }
    }

    private static void ensureColumn(Connection conn, Statement stmt, String tableName, String columnName,
            String columnDefinition) throws Exception {
        if (!columnExists(conn, tableName, columnName)) {
            System.out.println("   [FIX] Adding missing column: " + tableName + "." + columnName);
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }
}
