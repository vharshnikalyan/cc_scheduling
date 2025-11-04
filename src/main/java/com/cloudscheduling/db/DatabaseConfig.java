package com.cloudscheduling.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {

    // ✅ Update these to match your pgAdmin settings
    private static final String URL = "jdbc:postgresql://localhost:5432/cloud_scheduling";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "1234"; // replace if you have a different one

    static {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ PostgreSQL JDBC Driver Registered!");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ PostgreSQL JDBC Driver not found: " + e.getMessage());
        }
    }

    public static Connection getConnection() {
        try {
            Properties props = new Properties();
            props.setProperty("user", USERNAME);
            props.setProperty("password", PASSWORD);
            props.setProperty("ssl", "false");

            Connection connection = DriverManager.getConnection(URL, props);
            System.out.println("✅ Database connection established successfully!");
            return connection;
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect to database: " + e.getMessage());
            System.out.println("⚠️ Using in-memory data instead (if available).");
            return null;
        }
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("✅ Database connection closed!");
            } catch (SQLException e) {
                System.err.println("❌ Error closing database connection: " + e.getMessage());
            }
        }
    }

    public static void testConnection() {
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("✅ Database connection test: SUCCESS");
                System.out.println("   URL: " + URL);
                System.out.println("   User: " + USERNAME);
            } else {
                System.out.println("❌ Database connection test: FAILED");
            }
        } catch (SQLException e) {
            System.err.println("❌ Database connection test failed: " + e.getMessage());
        }
    }
}
