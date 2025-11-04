package com.cloudscheduling.algorithm;

import com.cloudscheduling.model.CloudJob;

import java.util.List;

/**
 * Minimal PBFScheduler stub. Adjust algorithmic details as needed.
 */
public class PBFScheduler {

    /**
     * Example method to schedule jobs: set start times sequentially on a single VM.
     * This is just a placeholder to match calls to setStartTime/getStartTime.
     */
    public void scheduleSequentially(List<CloudJob> jobs, double vmMips) {
        double currentTime = 0.0;
        for (CloudJob job : jobs) {
            job.setStartTime(currentTime);
            double executionTime = job.getLength() / vmMips;
            job.setFinishTime(currentTime + executionTime);
            currentTime += executionTime;
        }
    }

    /**
     * A comparator helper that might be used in your code to sort by startTime.
     */
    public static int compareByStartTime(CloudJob a, CloudJob b) {
        return Double.compare(a.getStartTime(), b.getStartTime());
    }
}
