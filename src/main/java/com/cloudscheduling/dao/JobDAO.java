package com.cloudscheduling.dao;

import com.cloudscheduling.db.DatabaseConfig;
import com.cloudscheduling.model.CloudJob;

import java.sql.*;
import java.util.*;

public class JobDAO {
    private Connection getConnection() throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/cloud_scheduling";
        String user = "postgres";
        String password = "1234";
        return DriverManager.getConnection(url, user, password);
    }

    // ✅ Fetch up to n jobs from database
    public List<CloudJob> getNJobs(int n) {
        List<CloudJob> jobs = new ArrayList<>();
        String sql = "SELECT job_id, job_name, job_length, priority, arrival_time, due_date, finish_time " +
                     "FROM cloud_jobs ORDER BY job_id LIMIT ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, n);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                CloudJob job = new CloudJob(
                    rs.getInt("job_id"),
                    rs.getString("job_name"),
                    rs.getLong("job_length"),
                    rs.getInt("priority"),
                    rs.getObject("arrival_time") != null ? rs.getDouble("arrival_time") : null,
                    rs.getObject("due_date") != null ? rs.getDouble("due_date") : null,
                    rs.getObject("finish_time") != null ? rs.getDouble("finish_time") : null
                );
                jobs.add(job);
            }
            System.out.println("✅ Loaded " + jobs.size() + " jobs from PostgreSQL.");
        } catch (SQLException e) {
            System.err.println("❌ Error loading jobs: " + e.getMessage());
        }
        return jobs;
    }

    // ✅ Get all jobs - using the simpler approach that matches your CloudJob constructor
    public List<CloudJob> getAllJobs() throws SQLException {
        List<CloudJob> jobs = new ArrayList<>();
        
        String sql = "SELECT job_id, job_name, job_length, priority, arrival_time, due_date, finish_time " +
                     "FROM cloud_jobs ORDER BY job_id LIMIT 1000";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                CloudJob job = new CloudJob(
                    rs.getInt("job_id"),
                    rs.getString("job_name"),
                    rs.getLong("job_length"),
                    rs.getInt("priority"),
                    rs.getObject("arrival_time") != null ? rs.getDouble("arrival_time") : null,
                    rs.getObject("due_date") != null ? rs.getDouble("due_date") : null,
                    rs.getObject("finish_time") != null ? rs.getDouble("finish_time") : null
                );
                jobs.add(job);
            }
        }
        return jobs;
    }

    // ✅ Get job deadlines (using due_date)
    public Map<Integer, Double> getJobDeadlines() throws SQLException {
        Map<Integer, Double> deadlines = new HashMap<>();
        String sql = "SELECT job_id, due_date FROM cloud_jobs WHERE due_date IS NOT NULL";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                deadlines.put(rs.getInt("job_id"), rs.getDouble("due_date"));
            }
        }
        return deadlines;
    }

    // ✅ Update completion times for finished jobs
    public void updateJobCompletionTimes(Map<Integer, Double> completionTimes) {
        if (completionTimes == null || completionTimes.isEmpty()) {
            System.out.println("⚠️ No completion times to update.");
            return;
        }

        String sql = "UPDATE cloud_jobs SET finish_time = ? WHERE job_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (Map.Entry<Integer, Double> entry : completionTimes.entrySet()) {
                if (entry.getValue() == null) {
                    ps.setNull(1, Types.DOUBLE);
                } else {
                    ps.setDouble(1, entry.getValue());
                }
                ps.setInt(2, entry.getKey());
                ps.addBatch();
            }

            int[] results = ps.executeBatch();
            System.out.println("✅ Updated " + results.length + " job finish times in PostgreSQL.");
        } catch (SQLException e) {
            System.err.println("❌ Error updating finish times: " + e.getMessage());
        }
    }

    // ✅ Get mapping from job_name -> job_id
    public Map<String, Integer> getJobIdMapping() {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT job_id, job_name FROM cloud_jobs";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("job_name"), rs.getInt("job_id"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error fetching job mapping: " + e.getMessage());
        }
        return map;
    }

    // ✅ Helper: builds a mapping from cloudlet index (0..N-1) -> job_id
    public Map<Integer, Integer> getCloudletIndexToJobId() {
        Map<Integer, Integer> map = new HashMap<>();
        String sql = "SELECT job_id FROM cloud_jobs ORDER BY job_id";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int idx = 0;
            while (rs.next()) {
                map.put(idx++, rs.getInt("job_id"));
            }
            System.out.println("✅ Built cloudletIndex->jobId map for " + map.size() + " rows.");
        } catch (SQLException e) {
            System.err.println("❌ Error building cloudlet index map: " + e.getMessage());
        }
        return map;
    }

    // ✅ Initialize deadlines if needed
    public void initializeDeadlinesIfNeeded() throws SQLException {
        String checkSql = "SELECT due_date FROM cloud_jobs LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(checkSql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (!rs.next() || rs.getObject("due_date") == null) {
                // Deadlines don't exist, create them
                updateJobDeadlines();
            }
        }
    }

    // ✅ Update job deadlines in database
    public void updateJobDeadlines() throws SQLException {
        String sql = "UPDATE cloud_jobs SET due_date = job_length / 500.0 * (1.5 + RANDOM())";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int updated = pstmt.executeUpdate();
            System.out.println("✅ Updated " + updated + " job deadlines in database");
        }
    }
}