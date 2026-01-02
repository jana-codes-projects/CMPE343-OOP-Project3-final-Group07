package com.cmpe343.db;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * Database connection utility class.
 * Provides MySQL database connections using configuration from app.properties.
 * Implements connection pooling via driver manager.
 * 
 * @author Group07
 * @version 1.0
 */
public class Db {
    private static Properties props;

    private static Properties props() {
        if (props != null)
            return props;
        try {
            props = new Properties();
            props.load(new FileInputStream("app.properties"));
            return props;
        } catch (Exception e) {
            throw new RuntimeException("app.properties okunamadı (proje kökünde olmalı).", e);
        }
    }

    public static Connection getConnection() {
        try {
            Properties p = props();
            return DriverManager.getConnection(
                    p.getProperty("db.url"),
                    p.getProperty("db.user"),
                    p.getProperty("db.password"));
        } catch (Exception e) {
            throw new RuntimeException("DB bağlantısı kurulamadı.", e);
        }
    }
}
