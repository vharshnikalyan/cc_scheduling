package com.cloudscheduling.cloudsim;

import com.cloudscheduling.model.CloudJob;

public class CloudJobTest {
    public static void main(String[] args) {
        CloudJob job = new CloudJob(1, "TestJob", 500L, 1, 0.0, 100.0, null);
        System.out.println("✅ CloudJob loaded successfully: " + job.getJobName());
        System.out.println("    toString: " + job.toString());
    }
}
