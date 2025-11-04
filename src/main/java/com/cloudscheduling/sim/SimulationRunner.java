package com.cloudscheduling.sim;

import com.cloudscheduling.dao.JobDAO;
import com.cloudscheduling.model.CloudJob;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.vms.Vm;
import com.cloudscheduling.sim.SimulationUtils;
import java.util.*;
import java.util.stream.Collectors;

public class SimulationRunner {

    public static void main(String[] args) {
        // ------- CONFIG -------
        int numJobsToLoad = 100;   // change if needed
        int cloudletPes = 1;       // PEs per cloudlet
        // ----------------------

        JobDAO jobDao = new JobDAO();

        // Load jobs from DB.
        // Use getNJobs(n) (or replace with getAllJobs() if that exists in your updated DAO)
        List<CloudJob> jobs = jobDao.getNJobs(numJobsToLoad);

        if (jobs.isEmpty()) {
            System.err.println("❌ No jobs loaded. Check DB / JobDAO.");
            return;
        }
        System.out.println("✅ Loaded " + jobs.size() + " jobs from DB.");

        // Create simulation and broker
        CloudSim sim = new CloudSim();
        DatacenterSimple datacenter = SimulationUtils.createDatacenter(sim);
        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(sim);

        // Create VMs (example simple VMs — replace with your VM creation)
        List<Vm> vmList = SimulationUtils.createVmList(); // <-- replace with your VM creation routine
        broker.submitVmList(vmList);

        // Create cloudlets and keep mapping Cloudlet -> CloudJob
        List<Cloudlet> cloudlets = new ArrayList<>();
        Map<Cloudlet, CloudJob> cloudletToJob = new HashMap<>();
        for (CloudJob job : jobs) {
            Cloudlet c = new CloudletSimple(job.getJobLength(), cloudletPes);
            // do NOT call c.setName(...) — some CloudSim versions don't have it
            cloudlets.add(c);
            cloudletToJob.put(c, job);
        }

        // Submit cloudlets
        broker.submitCloudletList(cloudlets);

        // Run simulation
        sim.start();

        // Collect finished cloudlets
        List<Cloudlet> finished = broker.getCloudletFinishedList();
        System.out.println("✅ Finished cloudlets: " + finished.size() + " / " + cloudlets.size());

        // Build jobId -> finish_time map for DB update
        Map<Integer, Double> completionTimes = new HashMap<>();
        double makespan = 0.0;
        double totalFlowTime = 0.0;
        double totalExecTime = 0.0;

        for (Cloudlet f : finished) {
            CloudJob j = cloudletToJob.get(f);
            if (j == null) {
                // Shouldn't happen if mapping was kept while JVM runs; log just in case.
                System.err.println("⚠️ No CloudJob mapping for cloudlet id: " + f.getId());
                continue;
            }
            int jobId = j.getJobId();
            double finish = f.getFinishTime();
            completionTimes.put(jobId, finish);

            makespan = Math.max(makespan, finish);
            totalExecTime += f.getActualCpuTime(); // cloudlet exec time
            // Flow time = finish - arrival (use job arrival if available)
            double arrival = j.getArrivalTime() != null ? j.getArrivalTime() : 0.0;
            totalFlowTime += (finish - arrival);
        }

        // Update DB finish times
        if (!completionTimes.isEmpty()) {
            jobDao.updateJobCompletionTimes(completionTimes);
            System.out.println("✅ Updated " + completionTimes.size() + " job finish times in DB.");
        } else {
            System.err.println("⚠️ No completion times to update.");
        }

        // Print simple metrics
        int completed = finished.size();
        double avgExecTime = completed > 0 ? totalExecTime / completed : 0.0;
        System.out.println("-------------------------------------------------");
        System.out.printf("Makespan: %.2f seconds\n", makespan);
        System.out.printf("Total Flow Time: %.2f seconds\n", totalFlowTime);
        System.out.printf("Avg Exec Time: %.2f seconds\n", avgExecTime);
        System.out.println("Completed: " + completed + "/" + cloudlets.size());
        System.out.println("-------------------------------------------------");
    }
}
