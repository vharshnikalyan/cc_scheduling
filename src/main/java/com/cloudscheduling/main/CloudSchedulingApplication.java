package com.cloudscheduling.main;

import com.cloudscheduling.cloudsim.OptimizedEGSJF;
import com.cloudscheduling.db.DatabaseConfig;
import com.cloudscheduling.dao.JobDAO;

public class CloudSchedulingApplication {

    public static void main(String[] args) {
        System.out.println("🚀 Cloud Scheduling Application - PostgreSQL Connected Version");
        System.out.println("==============================================================");

        // Check DB connection
        DatabaseConfig.testConnection();
        

        // Load & print a quick sample (optional)
        JobDAO jobDAO = new JobDAO();
        System.out.println("🔎 Loading 5 sample jobs as a quick check...");
        jobDAO.getNJobs(5).forEach(j ->
            System.out.println("   • " + j.getJobName() + " | Length: " + j.getJobLength() + " | Priority: " + j.getPriority())
        );

        // Run CloudSim simulation (this will read 100 jobs from DB by default)
        System.out.println("\n▶️ Starting CloudSim simulation (PBFS vs EG-SJF) ...");
        new OptimizedEGSJF();

        System.out.println("\n🎉 APPLICATION FINISHED");
    }
}
