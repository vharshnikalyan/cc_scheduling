package com.cloudscheduling.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultsDAO {
    // PostgreSQL connection parameters
    private static final String URL = "jdbc:postgresql://localhost:5432/cloud_scheduling";
    private static final String USER = "postgres";
    private static final String PASSWORD = "1234";
    
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Initialize deadlines in the database if they don't exist
     */
    /**
 * Initialize deadlines in the database if they don't exist - UPDATED FOR YOUR SCHEMA
 */
public void initializeDeadlines() {
    try (Connection conn = getConnection()) {
        // Check if due_date column has values
        String checkSql = "SELECT COUNT(*) as count_with_due_date FROM cloud_jobs WHERE due_date IS NOT NULL";
        
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             ResultSet rs = checkStmt.executeQuery()) {
            
            if (rs.next() && rs.getInt("count_with_due_date") == 0) {
                // Initialize due dates based on job length
                String updateSql = "UPDATE cloud_jobs SET due_date = job_length / 500.0 * (1.5 + RANDOM() * 1.0)";
                try (Statement updateStmt = conn.createStatement()) {
                    int updated = updateStmt.executeUpdate(updateSql);
                    System.out.println("✅ Initialized due dates for " + updated + " jobs");
                }
            } else {
                System.out.println("✅ Due dates already exist in database");
            }
        }
        
    } catch (SQLException e) {
        System.err.println("❌ Error initializing due dates: " + e.getMessage());
    }
}

/**
 * Calculate total tardiness for completed jobs - UPDATED FOR YOUR SCHEMA
 */
public double calculateTotalTardiness1() {
    double totalTardiness = 0.0;
    int lateJobs = 0;
    int totalProcessedJobs = 0;
    
    // Using cloud_jobs table with due_date column from your schema
    String sql = "SELECT job_id, job_length, finish_time, due_date " +
                 "FROM cloud_jobs " +
                 "WHERE finish_time IS NOT NULL AND due_date IS NOT NULL";
    
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql);
         ResultSet rs = pstmt.executeQuery()) {
        
        while (rs.next()) {
            totalProcessedJobs++;
            double finishTime = rs.getDouble("finish_time");
            double dueDate = rs.getDouble("due_date");
            
            double tardiness = Math.max(0, finishTime - dueDate);
            totalTardiness += tardiness;
            
            if (tardiness > 0) {
                lateJobs++;
            }
        }
        
        System.out.println("\n📊 TARDINESS CALCULATION SUMMARY:");
        System.out.println("   - Total Jobs Processed: " + totalProcessedJobs);
        System.out.println("   - Late Jobs: " + lateJobs + " (" + 
            (totalProcessedJobs > 0 ? String.format("%.1f", lateJobs * 100.0 / totalProcessedJobs) : 0) + "%)");
        System.out.println("   - Total Tardiness: " + String.format("%.2f", totalTardiness) + " seconds");
        System.out.println("   - Average Tardiness: " + 
            String.format("%.2f", (lateJobs > 0 ? totalTardiness / lateJobs : 0)) + " seconds");
        
    } catch (SQLException e) {
        System.err.println("❌ Error calculating total tardiness: " + e.getMessage());
        e.printStackTrace();
    }
    
    return totalTardiness;
}

/**
 * Update job completion times in the database - UPDATED FOR YOUR SCHEMA
 */
public void updateJobCompletionTimes1(Map<Integer, Double> completionTimes) {
    if (completionTimes == null || completionTimes.isEmpty()) {
        System.out.println("⚠️ No completion times to update");
        return;
    }
    
    String sql = "UPDATE cloud_jobs SET finish_time = ? WHERE job_id = ?";
    
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        
        int updatedCount = 0;
        for (Map.Entry<Integer, Double> entry : completionTimes.entrySet()) {
            pstmt.setDouble(1, entry.getValue());
            pstmt.setInt(2, entry.getKey());
            pstmt.addBatch();
            updatedCount++;
        }
        
        int[] results = pstmt.executeBatch();
        System.out.println("✅ Updated completion times for " + updatedCount + " jobs");
        
    } catch (SQLException e) {
        System.err.println("❌ Error updating job completion times: " + e.getMessage());
    }
}

/**
 * Get job deadlines for scheduling - UPDATED FOR YOUR SCHEMA
 */
public Map<Integer, Double> getJobDeadlines1() {
    Map<Integer, Double> deadlines = new HashMap<>();
    String sql = "SELECT job_id, due_date FROM cloud_jobs WHERE due_date IS NOT NULL";
    
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql);
         ResultSet rs = pstmt.executeQuery()) {
        
        while (rs.next()) {
            deadlines.put(rs.getInt("job_id"), rs.getDouble("due_date"));
        }
        System.out.println("✅ Loaded " + deadlines.size() + " job deadlines from database");
        
    } catch (SQLException e) {
        System.err.println("❌ Error loading job deadlines: " + e.getMessage());
    }
    return deadlines;
}

/**
 * Debug method to check current state of jobs and deadlines - UPDATED
 */
public void debugTardinessCalculation1() {
    String sql = "SELECT " +
                 "COUNT(*) as total_jobs, " +
                 "COUNT(finish_time) as jobs_with_finish_time, " +
                 "AVG(due_date) as avg_due_date, " +
                 "AVG(finish_time) as avg_finish_time, " +
                 "COUNT(CASE WHEN finish_time > due_date THEN 1 END) as late_jobs, " +
                 "COUNT(CASE WHEN due_date IS NULL THEN 1 END) as jobs_without_due_date " +
                 "FROM cloud_jobs";
    
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql);
         ResultSet rs = pstmt.executeQuery()) {
        
        if (rs.next()) {
            System.out.println("\n🔍 TARDINESS DEBUG INFO:");
            System.out.println("==========================================");
            System.out.println("   Total Jobs: " + rs.getInt("total_jobs"));
            System.out.println("   Jobs with Finish Time: " + rs.getInt("jobs_with_finish_time"));
            System.out.println("   Jobs without Due Date: " + rs.getInt("jobs_without_due_date"));
            System.out.println("   Average Due Date: " + String.format("%.2f", rs.getDouble("avg_due_date")));
            System.out.println("   Average Finish Time: " + String.format("%.2f", rs.getDouble("avg_finish_time")));
            System.out.println("   Late Jobs: " + rs.getInt("late_jobs"));
            System.out.println("==========================================");
        }
        
    } catch (SQLException e) {
        System.err.println("❌ Error in tardiness debug: " + e.getMessage());
    }
}

    /**
     * Debug method to check current state of jobs and deadlines
     */
    public void debugTardinessCalculation() {
        String sql = "SELECT " +
                     "COUNT(*) as total_jobs, " +
                     "COUNT(finish_time) as jobs_with_finish_time, " +
                     "AVG(deadline) as avg_deadline, " +
                     "AVG(finish_time) as avg_finish_time, " +
                     "COUNT(CASE WHEN finish_time > deadline THEN 1 END) as late_jobs, " +
                     "COUNT(CASE WHEN deadline IS NULL THEN 1 END) as jobs_without_deadline " +
                     "FROM cloud_jobs";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                System.out.println("\n🔍 TARDINESS DEBUG INFO:");
                System.out.println("==========================================");
                System.out.println("   Total Jobs: " + rs.getInt("total_jobs"));
                System.out.println("   Jobs with Finish Time: " + rs.getInt("jobs_with_finish_time"));
                System.out.println("   Jobs without Deadline: " + rs.getInt("jobs_without_deadline"));
                System.out.println("   Average Deadline: " + String.format("%.2f", rs.getDouble("avg_deadline")));
                System.out.println("   Average Finish Time: " + String.format("%.2f", rs.getDouble("avg_finish_time")));
                System.out.println("   Late Jobs: " + rs.getInt("late_jobs"));
                System.out.println("==========================================");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error in tardiness debug: " + e.getMessage());
        }
    }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("✅ PostgreSQL connection established successfully!");
            System.out.println("   URL: " + URL);
            System.out.println("   User: " + USER);
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect to PostgreSQL: " + e.getMessage());
            return false;
        }
    }

    /**
     * Add missing total_tardiness column to simulation_results table
     */
    public void addTardinessColumn() {
        String sql = "ALTER TABLE simulation_results ADD COLUMN IF NOT EXISTS total_tardiness DOUBLE PRECISION NOT NULL DEFAULT 0";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(sql);
            System.out.println("✅ Added total_tardiness column to simulation_results table");
            
        } catch (SQLException e) {
            System.err.println("❌ Error adding tardiness column: " + e.getMessage());
        }
    }

    /**
     * Calculate total tardiness for completed jobs - FIXED VERSION
     */
    public double calculateTotalTardiness() {
        double totalTardiness = 0.0;
        int lateJobs = 0;
        int totalProcessedJobs = 0;
        
        // Using cloud_jobs table which has both finish_time and deadline
        String sql = "SELECT job_id, job_length, finish_time, deadline " +
                     "FROM cloud_jobs " +
                     "WHERE finish_time IS NOT NULL AND deadline IS NOT NULL";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                totalProcessedJobs++;
                double finishTime = rs.getDouble("finish_time");
                double deadline = rs.getDouble("deadline");
                
                double tardiness = Math.max(0, finishTime - deadline);
                totalTardiness += tardiness;
                
                if (tardiness > 0) {
                    lateJobs++;
                }
                
                // Debug output for first few jobs
                if (totalProcessedJobs <= 5) {
                    System.out.printf("   Job %d: Finish=%.2f, Deadline=%.2f, Tardiness=%.2f%n",
                        rs.getInt("job_id"), finishTime, deadline, tardiness);
                }
            }
            
            System.out.println("\n📊 TARDINESS CALCULATION SUMMARY:");
            System.out.println("   - Total Jobs Processed: " + totalProcessedJobs);
            System.out.println("   - Late Jobs: " + lateJobs + " (" + 
                (totalProcessedJobs > 0 ? String.format("%.1f", lateJobs * 100.0 / totalProcessedJobs) : 0) + "%)");
            System.out.println("   - Total Tardiness: " + String.format("%.2f", totalTardiness) + " seconds");
            System.out.println("   - Average Tardiness: " + 
                String.format("%.2f", (lateJobs > 0 ? totalTardiness / lateJobs : 0)) + " seconds");
            
        } catch (SQLException e) {
            System.err.println("❌ Error calculating total tardiness: " + e.getMessage());
            e.printStackTrace();
        }
        
        return totalTardiness;
    }

    /**
     * Update job completion times in the database
     */
    public void updateJobCompletionTimes(Map<Integer, Double> completionTimes) {
        if (completionTimes == null || completionTimes.isEmpty()) {
            System.out.println("⚠️ No completion times to update");
            return;
        }
        
        String sql = "UPDATE cloud_jobs SET finish_time = ? WHERE job_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            int updatedCount = 0;
            for (Map.Entry<Integer, Double> entry : completionTimes.entrySet()) {
                pstmt.setDouble(1, entry.getValue());
                pstmt.setInt(2, entry.getKey());
                pstmt.addBatch();
                updatedCount++;
            }
            
            int[] results = pstmt.executeBatch();
            System.out.println("✅ Updated completion times for " + updatedCount + " jobs");
            
        } catch (SQLException e) {
            System.err.println("❌ Error updating job completion times: " + e.getMessage());
        }
    }

    /**
     * Enhanced method to save simulation results with tardiness
     */
    public void saveSimulationResultsWithTardiness(String algorithmName, double makespan, 
                                                  double totalFlowTime, double avgExecutionTime,
                                                  double avgWaitingTime, int jobsCompleted, 
                                                  int totalJobs) {
        
        double totalTardiness = calculateTotalTardiness1();
        
        String sql = "INSERT INTO simulation_results " +
                     "(algorithm_name, makespan, total_flow_time, avg_execution_time, " +
                     "avg_waiting_time, jobs_completed, total_jobs, total_tardiness) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, algorithmName);
            pstmt.setDouble(2, makespan);
            pstmt.setDouble(3, totalFlowTime);
            pstmt.setDouble(4, avgExecutionTime);
            pstmt.setDouble(5, avgWaitingTime);
            pstmt.setInt(6, jobsCompleted);
            pstmt.setInt(7, totalJobs);
            pstmt.setDouble(8, totalTardiness);
            
            pstmt.executeUpdate();
            System.out.println("✅ Simulation results saved with tardiness: " + totalTardiness);
            
        } catch (SQLException e) {
            System.err.println("❌ Error saving simulation results: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get historical results with tardiness
     */
    public void displayHistoricalResults() {
        String sql = "SELECT algorithm_name, makespan, total_flow_time, avg_execution_time, " +
                     "total_tardiness, jobs_completed, total_jobs, simulation_timestamp " +
                     "FROM simulation_results ORDER BY simulation_timestamp DESC LIMIT 10";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            System.out.println("\n📈 HISTORICAL RESULTS WITH TARDINESS (Last 10 simulations):");
            System.out.println("====================================================================================================");
            System.out.printf("%-12s %-10s %-12s %-10s %-15s %-10s %s%n", 
                "Algorithm", "Makespan", "Total Flow", "Avg Exec", "Total Tardiness", "Completed", "Timestamp");
            System.out.println("====================================================================================================");
            
            boolean hasResults = false;
            while (rs.next()) {
                hasResults = true;
                System.out.printf("%-12s %-10.2f %-12.2f %-10.2f %-15.2f %-10s %s%n",
                    rs.getString("algorithm_name"),
                    rs.getDouble("makespan"),
                    rs.getDouble("total_flow_time"),
                    rs.getDouble("avg_execution_time"),
                    rs.getDouble("total_tardiness"),
                    rs.getInt("jobs_completed") + "/" + rs.getInt("total_jobs"),
                    rs.getTimestamp("simulation_timestamp")
                );
            }
            
            if (!hasResults) {
                System.out.println("   No historical results found in database");
            }
            System.out.println("====================================================================================================");
            
        } catch (SQLException e) {
            System.err.println("❌ Error retrieving historical results: " + e.getMessage());
        }
    }

    /**
     * Original method to save simulation results (maintains backward compatibility)
     */
    public void saveSimulationResult(String algorithmName, double makespan, 
                                    double totalFlowTime, double avgExecutionTime,
                                    double avgWaitingTime, int jobsCompleted, 
                                    int totalJobs) throws Exception {
        saveSimulationResultsWithTardiness(algorithmName, makespan, totalFlowTime, 
                                         avgExecutionTime, avgWaitingTime, jobsCompleted, totalJobs);
    }

    /**
     * Get all simulation results
     */
    public List<String> getAllSimulationResults() {
        List<String> results = new ArrayList<>();
        String sql = "SELECT * FROM simulation_results ORDER BY simulation_timestamp DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                String result = String.format("Algorithm: %s, Makespan: %.2f, Tardiness: %.2f, Completed: %d/%d",
                    rs.getString("algorithm_name"),
                    rs.getDouble("makespan"),
                    rs.getDouble("total_tardiness"),
                    rs.getInt("jobs_completed"),
                    rs.getInt("total_jobs"));
                results.add(result);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error retrieving simulation results: " + e.getMessage());
        }
        return results;
    }

    /**
     * Clear all simulation results
     */
    public void clearAllResults() {
        String sql = "DELETE FROM simulation_results";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(sql);
            System.out.println("✅ Cleared all simulation results");
            
        } catch (SQLException e) {
            System.err.println("❌ Error clearing simulation results: " + e.getMessage());
        }
    }

    /**
     * Get results by algorithm
     */
    public List<String> getResultsByAlgorithm(String algorithmName) {
        List<String> results = new ArrayList<>();
        String sql = "SELECT * FROM simulation_results WHERE algorithm_name = ? ORDER BY simulation_timestamp DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, algorithmName);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String result = String.format("Algorithm: %s, Makespan: %.2f, Tardiness: %.2f, Completed: %d/%d, Time: %s",
                    rs.getString("algorithm_name"),
                    rs.getDouble("makespan"),
                    rs.getDouble("total_tardiness"),
                    rs.getInt("jobs_completed"),
                    rs.getInt("total_jobs"),
                    rs.getTimestamp("simulation_timestamp"));
                results.add(result);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error retrieving results by algorithm: " + e.getMessage());
        }
        return results;
    }

    /**
     * Get detailed tardiness analysis - FIXED VERSION
     */
    public void displayTardinessAnalysis() {
        String sql = "SELECT " +
                     "COUNT(*) as total_jobs, " +
                     "COUNT(CASE WHEN finish_time > deadline THEN 1 END) as late_jobs, " +
                     "COALESCE(SUM(GREATEST(0, finish_time - deadline)), 0) as total_tardiness, " +
                     "COALESCE(AVG(CASE WHEN finish_time > deadline THEN finish_time - deadline ELSE 0 END), 0) as avg_tardiness, " +
                     "COALESCE(MAX(GREATEST(0, finish_time - deadline)), 0) as max_tardiness " +
                     "FROM cloud_jobs WHERE finish_time IS NOT NULL AND deadline IS NOT NULL";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                int totalJobs = rs.getInt("total_jobs");
                int lateJobs = rs.getInt("late_jobs");
                double totalTardiness = rs.getDouble("total_tardiness");
                double avgTardiness = rs.getDouble("avg_tardiness");
                double maxTardiness = rs.getDouble("max_tardiness");
                double latePercentage = totalJobs > 0 ? (lateJobs * 100.0 / totalJobs) : 0;
                
                System.out.println("\n📊 DETAILED TARDINESS ANALYSIS:");
                System.out.println("==========================================");
                System.out.println("   Total Jobs Completed: " + totalJobs);
                System.out.println("   Late Jobs: " + lateJobs + " (" + String.format("%.1f", latePercentage) + "%)");
                System.out.println("   Total Tardiness: " + String.format("%.2f", totalTardiness) + " seconds");
                System.out.println("   Average Tardiness: " + String.format("%.2f", avgTardiness) + " seconds");
                System.out.println("   Maximum Tardiness: " + String.format("%.2f", maxTardiness) + " seconds");
                System.out.println("==========================================");
            } else {
                System.out.println("⚠️ No jobs with both finish_time and deadline found for tardiness analysis");
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error retrieving tardiness analysis: " + e.getMessage());
        }
    }

    /**
     * Get job deadlines for scheduling
     */
    public Map<Integer, Double> getJobDeadlines() {
        Map<Integer, Double> deadlines = new HashMap<>();
        String sql = "SELECT job_id, deadline FROM cloud_jobs WHERE deadline IS NOT NULL";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                deadlines.put(rs.getInt("job_id"), rs.getDouble("deadline"));
            }
            System.out.println("✅ Loaded " + deadlines.size() + " job deadlines from database");
            
        } catch (SQLException e) {
            System.err.println("❌ Error loading job deadlines: " + e.getMessage());
        }
        return deadlines;
    }
}