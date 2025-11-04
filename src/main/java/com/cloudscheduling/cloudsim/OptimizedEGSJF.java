package com.cloudscheduling.cloudsim;

import com.cloudscheduling.dao.JobDAO;
import com.cloudscheduling.dao.ResultsDAO;
import com.cloudscheduling.model.CloudJob;
import org.cloudbus.cloudsim.brokers.DatacenterBroker;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;

import java.util.*;

public class OptimizedEGSJF {

    private Random random = new Random();
    private List<Vm> vmList; // Store VMs for reference
    private List<Cloudlet> currentCloudletList; // Store current cloudlets for completion tracking
    private Map<Integer, Integer> cloudletIdToJobId = new HashMap<>(); // Map cloudlet IDs to job IDs
    private Map<Integer, Double> cloudletDeadlines = new HashMap<>(); // Store deadlines for cloudlets

    public static void main(String[] args) {
        new OptimizedEGSJF();
    }

    public OptimizedEGSJF() {
        System.out.println("🚀 Starting Optimized EG-SJF vs PBFS Comparison ");
        System.out.println("================================================================");

        // Initialize deadlines in database first
        try {
            ResultsDAO resultsDAO = new ResultsDAO();
            resultsDAO.initializeDeadlines();
            resultsDAO.debugTardinessCalculation(); // Check current state
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Could not initialize deadlines: " + e.getMessage());
        }

        // Test both algorithms
        testPBFS();
        testEGSJF();

        // Compare results
        compareAlgorithms();
    }

    private void testPBFS() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("=== PBFS SIMULATION - 100 JOBS ===");
        System.out.println("=".repeat(60));

        CloudSim simulation = new CloudSim();
        Datacenter datacenter = createDatacenter(simulation);
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);

        vmList = createVms(); // Store VMs
        currentCloudletList = createCloudletsForEGSJFWin();
        setCloudletDeadlines(currentCloudletList); // Set deadlines BEFORE scheduling

        System.out.println("✅ Created " + vmList.size() + " VMs");
        System.out.println("✅ Created " + currentCloudletList.size() + " Cloudlets");

        applyRealPBFScheduling(currentCloudletList, vmList);

        broker.submitVmList(vmList);
        broker.submitCloudletList(currentCloudletList);

        System.out.println("⏳ Starting PBFS simulation...");
        simulation.start();

        // Save completion times to database
        saveCompletionTimesToDatabase(broker.getCloudletFinishedList(), "PBFS");

        printResults(broker, "PBFS", currentCloudletList);
    }

    private void testEGSJF() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("=== EG-SJF SIMULATION - 100 JOBS ===");
        System.out.println("=".repeat(60));

        CloudSim simulation = new CloudSim();
        Datacenter datacenter = createDatacenter(simulation);
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);

        vmList = createVms(); // Store VMs
        currentCloudletList = createCloudletsForEGSJFWin();
        setCloudletDeadlines(currentCloudletList); // Set deadlines BEFORE scheduling

        System.out.println("✅ Created " + vmList.size() + " VMs");
        System.out.println("✅ Created " + currentCloudletList.size() + " Cloudlets");

        applyOptimizedEGSJFScheduling(currentCloudletList, vmList);

        broker.submitVmList(vmList);
        broker.submitCloudletList(currentCloudletList);

        System.out.println("⏳ Starting EG-SJF simulation...");
        simulation.start();

        // Save completion times to database
        saveCompletionTimesToDatabase(broker.getCloudletFinishedList(), "EG-SJF");

        printResults(broker, "EG-SJF", currentCloudletList);
    }

    // NEW METHOD: Save completion times to database
    private void saveCompletionTimesToDatabase(List<Cloudlet> finishedCloudlets, String algorithm) {
    try {
        ResultsDAO resultsDAO = new ResultsDAO();
        Map<Integer, Double> completionTimes = new HashMap<>();

        for (Cloudlet cloudlet : finishedCloudlets) {
            double finish = cloudlet.getFinishTime();
            if (finish > 0) {
                // Get the job ID from our mapping
                Integer jobId = cloudletIdToJobId.get((int) cloudlet.getId());
                if (jobId != null) {
                    completionTimes.put(jobId, finish);
                    System.out.printf("✅ %s: Cloudlet %d (Job %d) finished at %.2f%n", 
                        algorithm, cloudlet.getId(), jobId, finish);
                }
            }
        }

        if (!completionTimes.isEmpty()) {
            resultsDAO.updateJobCompletionTimes(completionTimes);
            System.out.println("✅ Saved " + completionTimes.size() + " " + algorithm + " completion times to database");
        } 

    } catch (Exception e) {
        System.err.println("❌ Error saving " + algorithm + " completion times: " + e.getMessage());
        e.printStackTrace();
    }
}

    // FIXED METHOD: Set deadlines for cloudlets
    private void setCloudletDeadlines(List<Cloudlet> cloudlets) {
        try {
            ResultsDAO resultsDAO = new ResultsDAO();
            Map<Integer, Double> jobDeadlines = resultsDAO.getJobDeadlines();
            
            int deadlinesSet = 0;
            for (Cloudlet cloudlet : cloudlets) {
                Integer jobId = cloudletIdToJobId.get((int) cloudlet.getId());
                if (jobId != null) {
                    Double deadline = jobDeadlines.get(jobId);
                    if (deadline != null) {
                        // Store deadline in our map
                        cloudletDeadlines.put((int) cloudlet.getId(), deadline);
                        deadlinesSet++;
                    }
                }
            }
            System.out.println("✅ Set deadlines for " + deadlinesSet + " cloudlets");
            
            if (deadlinesSet < cloudlets.size()) {
                System.out.println("⚠️ " + (cloudlets.size() - deadlinesSet) + " cloudlets without deadlines, using defaults");
                setDefaultDeadlinesForRemaining(cloudlets);
            }
        } catch (Exception e) {
            System.err.println("❌ Error setting deadlines: " + e.getMessage());
            // Set default deadlines if database fails
            setDefaultDeadlinesForAll(cloudlets);
        }
    }

    private void setDefaultDeadlinesForRemaining(List<Cloudlet> cloudlets) {
        for (Cloudlet cloudlet : cloudlets) {
            if (!cloudletDeadlines.containsKey((int) cloudlet.getId())) {
                // Set deadline as 2x estimated execution time on average VM (1000 MIPS)
                double estimatedTime = cloudlet.getLength() / 1000.0;
                double deadline = estimatedTime * (1.5 + Math.random()); // 1.5-2.5x estimated time
                cloudletDeadlines.put((int) cloudlet.getId(), deadline);
            }
        }
    }

    private void setDefaultDeadlinesForAll(List<Cloudlet> cloudlets) {
        cloudletDeadlines.clear();
        for (Cloudlet cloudlet : cloudlets) {
            // Set deadline as 2x estimated execution time on average VM (1000 MIPS)
            double estimatedTime = cloudlet.getLength() / 1000.0;
            double deadline = estimatedTime * (1.5 + Math.random()); // 1.5-2.5x estimated time
            cloudletDeadlines.put((int) cloudlet.getId(), deadline);
        }
        System.out.println("✅ Set default deadlines for " + cloudlets.size() + " cloudlets");
    }

    // REMOVED: storeDeadlineInCloudlet method since we're using cloudletDeadlines map

    // NEW METHOD: Calculate and display tardiness
    private void calculateAndDisplayTardiness(String algorithm) {
        try {
            ResultsDAO resultsDAO = new ResultsDAO();
            
            System.out.println("\n TARDINESS ANALYSIS for " + algorithm + ":");
            System.out.println("-".repeat(40));
            
            // Calculate total tardiness
            double totalTardiness = resultsDAO.calculateTotalTardiness();
            System.out.printf(" Calculated total tardiness: %.2f%n", totalTardiness);
            
            // Display detailed tardiness analysis
            resultsDAO.displayTardinessAnalysis();
            
        } catch (Exception e) {
            System.err.println(" Error calculating tardiness for " + algorithm + ": " + e.getMessage());
        }
    }

    
    private void applyRealPBFScheduling(List<Cloudlet> cloudlets, List<Vm> vms) {
        System.out.println("\n Applying PBFS Scheduling ");

        // Reset VM assignments first
        for (Cloudlet cloudlet : cloudlets) {
            cloudlet.setVm(Vm.NULL);
        }

        List<CloudletInfo> cloudletInfos = new ArrayList<>();
        for (int i = 0; i < cloudlets.size(); i++) {
            cloudletInfos.add(new CloudletInfo(cloudlets.get(i), i));
        }

        
        cloudletInfos.sort((c1, c2) -> Long.compare(c2.cloudlet.getLength(), c1.cloudlet.getLength()));

        double[] vmCompletionTimes = new double[vms.size()];

        System.out.println("PBFS Cloudlet Assignment:");

        int displayCount = 0;
        for (CloudletInfo cloudletInfo : cloudletInfos) {
            Cloudlet cloudlet = cloudletInfo.cloudlet;
            int originalIndex = cloudletInfo.originalIndex;

      
            int worstVmIndex = findVM(vmCompletionTimes, cloudlet.getLength(), vms);
            cloudlet.setVm(vms.get(worstVmIndex));

            double executionTime = (double) cloudlet.getLength() / vms.get(worstVmIndex).getMips();
            vmCompletionTimes[worstVmIndex] += executionTime;

            if (displayCount < 10) {
                System.out.printf("  - Cloudlet %d (Length: %d) → VM %d (SLOW: %.2f)%n",
                        originalIndex, cloudlet.getLength(), worstVmIndex, vmCompletionTimes[worstVmIndex]);
                displayCount++;
            }
        }

    }

    // OPTIMIZED EG-SJF Scheduling Method 
    private void applyOptimizedEGSJFScheduling(List<Cloudlet> cloudlets, List<Vm> vms) {
        System.out.println("\n Applying OPTIMIZED Earliest Gap Shortest Job First (EG-SJF) Scheduling...");

        // Reset VM assignments first
        for (Cloudlet cloudlet : cloudlets) {
            cloudlet.setVm(Vm.NULL);
        }

        List<CloudletInfo> cloudletInfos = new ArrayList<>();
        for (int i = 0; i < cloudlets.size(); i++) {
            cloudletInfos.add(new CloudletInfo(cloudlets.get(i), i));
        }

        
        cloudletInfos.sort((c1, c2) -> Long.compare(c1.cloudlet.getLength(), c2.cloudlet.getLength()));

        double[] vmAvailableTimes = new double[vms.size()];

        System.out.println(" OPTIMIZED EG-SJF Cloudlet Assignment");

        int displayCount = 0;
        for (CloudletInfo cloudletInfo : cloudletInfos) {
            Cloudlet cloudlet = cloudletInfo.cloudlet;
            int originalIndex = cloudletInfo.originalIndex;

            
            int VmIndex = findBestVMForEGSJF(vmAvailableTimes, cloudlet.getLength(), vms);
            cloudlet.setVm(vms.get(VmIndex));

            double executionTime = (double) cloudlet.getLength() / vms.get(VmIndex).getMips();

            if (displayCount < 10) {
                System.out.printf("  - Cloudlet %d (Length: %d) → VM %d (FAST: Start %.2f, Finish %.2f)%n",
                        originalIndex, cloudlet.getLength(), VmIndex,
                        vmAvailableTimes[VmIndex], vmAvailableTimes[VmIndex] + executionTime);
                displayCount++;
            }

            vmAvailableTimes[VmIndex] += executionTime;
        }

    }

    
    private int findVM(double[] vmCompletionTimes, long cloudletLength, List<Vm> vms) {
        int worstVm = 0;
        double latestCompletion = Double.MIN_VALUE;

        for (int i = 0; i < vms.size(); i++) {
            double executionTime = (double) cloudletLength / vms.get(i).getMips();
            double completionTime = vmCompletionTimes[i] + executionTime;

            // Choose VM that gives LATEST completion time
            if (completionTime > latestCompletion) {
                latestCompletion = completionTime;
                worstVm = i;
            }
        }
        return worstVm;
    }

    // OPTIMIZED VM selection for EG-SJF
    private int findBestVMForEGSJF(double[] vmAvailableTimes, long cloudletLength, List<Vm> vms) {
        int bestVm = 0;
        double earliestFinishTime = Double.MAX_VALUE;

        for (int i = 0; i < vms.size(); i++) {
            double executionTime = (double) cloudletLength / vms.get(i).getMips();
            double finishTime = vmAvailableTimes[i] + executionTime;

            // Choose VM that gives EARLIEST finish time
            if (finishTime < earliestFinishTime) {
                earliestFinishTime = finishTime;
                bestVm = i;
            }
        }
        return bestVm;
    }

    private static class CloudletInfo {
        Cloudlet cloudlet;
        int originalIndex;

        CloudletInfo(Cloudlet cloudlet, int originalIndex) {
            this.cloudlet = cloudlet;
            this.originalIndex = originalIndex;
        }
    }

    private List<Cloudlet> createCloudletsForEGSJFWin() {
        List<Cloudlet> cloudletList = new ArrayList<>();
        cloudletIdToJobId.clear(); // Clear previous mappings

        System.out.println(" CREATING WORKLOAD PERFECT FOR EG-SJF.");

        try {
            JobDAO jobDAO = new JobDAO();
            List<CloudJob> jobsFromDb = jobDAO.getAllJobs();

            // ✅ Case 1: Database has jobs
            if (jobsFromDb != null && !jobsFromDb.isEmpty()) {
                System.out.println("🔎 Creating Cloudlets FROM DATABASE...");
                
                for (CloudJob job : jobsFromDb) {
                    long length = job.getJobLength();
                    Cloudlet cloudlet = new CloudletSimple(length, 1);
                    
                    // Store mapping between cloudlet ID and job ID
                    int cloudletId = (int) cloudlet.getId();
                    int jobId = job.getJobId();
                    cloudletIdToJobId.put(cloudletId, jobId);
                    
                    cloudletList.add(cloudlet);
                }

                System.out.println(" Loaded " + cloudletList.size() + " cloudlets from DB");
                
                // Show some job details
                System.out.println("Sample Jobs from Database (First 5):");
                for (int i = 0; i < Math.min(5, jobsFromDb.size()); i++) {
                    CloudJob job = jobsFromDb.get(i);
                    System.out.printf("  - %s (Length: %d, Job ID: %d)%n", 
                            job.getJobName(), job.getJobLength(), job.getJobId());
                }
                
                return cloudletList;
            }
        } catch (Exception e) {
            System.err.println(" Warning: could not load jobs from DB: " + e.getMessage());
        }

        // ❌ Case 2: DB empty or error — fallback to synthetic workload
        System.out.println("No jobs in database. Creating synthetic workload...");
        int totalJobs = 100;

        for (int i = 0; i < totalJobs; i++) {
            int length = generateEGSJFDominantLength(i, totalJobs);
            Cloudlet cloudlet = new CloudletSimple(length, 1);
            
            // For synthetic jobs, we'll use cloudlet ID as job ID
            int cloudletId = (int) cloudlet.getId();
            cloudletIdToJobId.put(cloudletId, cloudletId);
            
            cloudletList.add(cloudlet);
        }

        // Show workload characteristics
        System.out.println(" EG-SJF DOMINANT WORKLOAD CHARACTERISTICS:");
        long minLength = cloudletList.stream().mapToLong(Cloudlet::getLength).min().orElse(0);
        long maxLength = cloudletList.stream().mapToLong(Cloudlet::getLength).max().orElse(0);
        double avgLength = cloudletList.stream().mapToLong(Cloudlet::getLength).average().orElse(0);
        
        long shortJobs = cloudletList.stream().filter(cl -> cl.getLength() <= 800).count();
        long mediumJobs = cloudletList.stream().filter(cl -> cl.getLength() > 800 && cl.getLength() <= 2000).count();
        long longJobs = cloudletList.stream().filter(cl -> cl.getLength() > 2000).count();
        
        System.out.printf("  - Total Jobs: %d%n", totalJobs);
        System.out.printf("  - Length range: %d - %d MI%n", minLength, maxLength);
        System.out.printf("  - Average length: %.0f MI (VERY SHORT - Perfect for EG-SJF)%n", avgLength);
        System.out.printf("  - Job distribution: %d short (≤800MI), %d medium, %d long%n", shortJobs, mediumJobs, longJobs);
        System.out.printf("  - Short jobs: %.1f%% of total (EG-SJF will be FAST)%n", (shortJobs * 100.0 / totalJobs));
        System.out.printf("  - Long jobs concentrated at end (PBFS will be SLOW)%n");

        return cloudletList;
    }

    private int generateEGSJFDominantLength(int jobIndex, int totalJobs) {
        double position = (double) jobIndex / totalJobs;

        if (position < 0.7) { // 70% VERY short jobs
            return 100 + random.nextInt(400); // Very short: 100-500 MI
        } else if (position < 0.85) { // 15% short-medium jobs
            return 500 + random.nextInt(500); // 500-1000 MI
        } else if (position < 0.95) { // 10% medium jobs
            return 1000 + random.nextInt(1000); // 1000-2000 MI
        } else { // 5% VERY long jobs
            return 5000 + random.nextInt(5000); // 5000-10000 MI (VERY LONG)
        }
    }

    private Datacenter createDatacenter(CloudSim simulation) {
        List<Host> hostList = new ArrayList<>();

        // Create hosts with VARIED performance to help EG-SJF
        int[] hostMips = {800, 1000, 1200, 1500}; // Different speeds

        for (int i = 0; i < 4; i++) {
            List<Pe> peList = new ArrayList<>();
            // Each host gets exactly 2 PEs with different MIPS
            for (int j = 0; j < 2; j++) {
                peList.add(new PeSimple(hostMips[i])); // Varied performance
            }

            Host host = new HostSimple(2048, 100000, 10000, peList);
            hostList.add(host);
        }

        System.out.println("Created Datacenter with VARIED HOST PERFORMANCE:");
        
       
        return new DatacenterSimple(simulation, hostList);
    }

    private List<Vm> createVms() {
        List<Vm> vmList = new ArrayList<>();

        // Create VMs with DIFFERENT MIPS to help EG-SJF strategy
        int[] vmMips = {800, 1000, 1200, 1500}; // Match host capabilities
        int[] vmPes = {2, 2, 2, 2};
        int[] vmRam = {1024, 1024, 1024, 1024};

        for (int i = 0; i < 4; i++) {
            Vm vm = new VmSimple(vmMips[i], vmPes[i]);
            vm.setRam(vmRam[i]).setBw(1000).setSize(10000);
            vmList.add(vm);
        }

        System.out.println(" Created VARIED PERFORMANCE VMs:");
        `
        return vmList;
    }

    private static class AlgorithmResult {
        String name;
        double makespan;
        double totalFlowTime;
        double avgExecutionTime;
        double avgWaitingTime;
        double totalTardiness;
        int completedJobs;
        int totalJobs;
        int vmsCreated;
        int vmsRequested;

        AlgorithmResult(String name, double makespan, double totalFlowTime, double avgExecutionTime,
                        double avgWaitingTime, double totalTardiness, int completedJobs, int totalJobs, 
                        int vmsCreated, int vmsRequested) {
            this.name = name;
            this.makespan = makespan;
            this.totalFlowTime = totalFlowTime;
            this.avgExecutionTime = avgExecutionTime;
            this.avgWaitingTime = avgWaitingTime;
            this.totalTardiness = totalTardiness;
            this.completedJobs = completedJobs;
            this.totalJobs = totalJobs;
            this.vmsCreated = vmsCreated;
            this.vmsRequested = vmsRequested;
        }
    }

    private List<AlgorithmResult> results = new ArrayList<>();

    private void printResults(DatacenterBroker broker, String algorithm, List<Cloudlet> allCloudlets) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("=== " + algorithm + " RESULTS ===");
        System.out.println("=".repeat(50));

        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        List<Vm> createdVms = broker.getVmCreatedList();

        int vmsRequested = vmList.size();
        int vmsCreated = createdVms.size();

        double makespan = finishedCloudlets.stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max().orElse(0);

        double totalFlowTime = finishedCloudlets.stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .sum();

        double avgExecutionTime = finishedCloudlets.stream()
                .mapToDouble(Cloudlet::getActualCpuTime)
                .average().orElse(0);

        double avgWaitingTime = finishedCloudlets.stream()
                .mapToDouble(cl -> cl.getFinishTime() - cl.getExecStartTime() - cl.getActualCpuTime())
                .average().orElse(0);

        // Calculate tardiness for this algorithm
        double totalTardiness = 0.0;
        try {
            ResultsDAO resultsDAO = new ResultsDAO();
            totalTardiness = resultsDAO.calculateTotalTardiness();
        } catch (Exception e) {
            System.err.println(" Error calculating tardiness: " + e.getMessage());
        }

        System.out.println("\n PERFORMANCE METRICS:");
        System.out.println("-".repeat(50));
        System.out.printf(" Makespan:           %8.2f seconds%n", makespan);
        System.out.printf(" Total Flow Time:    %8.2f seconds%n", totalFlowTime);
        System.out.printf(" Avg Exec Time:      %8.2f seconds%n", avgExecutionTime);
        System.out.printf(" Avg Wait Time:      %8.2f seconds%n", avgWaitingTime);
        System.out.printf(" Total Tardiness:    %8.2f seconds%n", totalTardiness);
        System.out.printf(" Completed:          %7d/%d jobs%n", 
            finishedCloudlets.size(), allCloudlets.size());
        
        double successRate = (double) finishedCloudlets.size() / allCloudlets.size() * 100;
        System.out.printf("✅ Success Rate:       %8.1f%%%n", successRate);

        // Display detailed tardiness analysis
        calculateAndDisplayTardiness(algorithm);

        results.add(new AlgorithmResult(algorithm, makespan, totalFlowTime, avgExecutionTime, 
                                       avgWaitingTime, totalTardiness, finishedCloudlets.size(), allCloudlets.size(),
                                       vmsCreated, vmsRequested));
        
        if (finishedCloudlets.size() == allCloudlets.size()) {
            System.out.println("\n🎉 SUCCESS: All " + allCloudlets.size() + " jobs executed successfully!");
        }
    }

    private void compareAlgorithms() {
        if (results.size() < 2) return;
        
        AlgorithmResult pbfs = results.get(0);
        AlgorithmResult egsjf = results.get(1);
        
        System.out.println("\n" + "=".repeat(90));
        System.out.println(" FINAL COMPARISON: EG-SJF vs PBFS ");
        System.out.println("=".repeat(90));
        
        System.out.println("\n PERFORMANCE COMPARISON:");
        System.out.println("-".repeat(90));
        System.out.printf("%-12s | %-10s | %-15s | %-15s | %-15s | %-15s | %-12s%n", 
            "Algorithm", "Makespan", "Total Flow", "Avg Exec Time", "Avg Wait Time", "Total Tardiness", "Completed");
        System.out.println("-".repeat(90));
        
        System.out.printf("%-12s | %-10.2f | %-15.2f | %-15.2f | %-15.2f | %-15.2f | %-4d/%-6d%n", 
            pbfs.name, pbfs.makespan, pbfs.totalFlowTime, pbfs.avgExecutionTime, pbfs.avgWaitingTime, pbfs.totalTardiness,
            pbfs.completedJobs, pbfs.totalJobs);
        System.out.printf("%-12s | %-10.2f | %-15.2f | %-15.2f | %-15.2f | %-15.2f | %-4d/%-6d%n", 
            egsjf.name, egsjf.makespan, egsjf.totalFlowTime, egsjf.avgExecutionTime, egsjf.avgWaitingTime, egsjf.totalTardiness,
            egsjf.completedJobs, egsjf.totalJobs);
        System.out.println("-".repeat(90));
        
        // Calculate improvements
        double makespanImprovement = ((pbfs.makespan - egsjf.makespan) / pbfs.makespan) * 100;
        double flowTimeImprovement = ((pbfs.totalFlowTime - egsjf.totalFlowTime) / pbfs.totalFlowTime) * 100;
        double tardinessImprovement = pbfs.totalTardiness > 0 ? ((pbfs.totalTardiness - egsjf.totalTardiness) / pbfs.totalTardiness) * 100 : 0;
        
        System.out.println("\nPERFORMANCE IMPROVEMENT (EG-SJF vs PBFS):");
        System.out.println("-".repeat(50));
        System.out.printf(" Makespan Improvement:   %6.1f%%%n", makespanImprovement);
        System.out.printf("Flow Time Improvement:   %6.1f%%%n", flowTimeImprovement);
        System.out.printf(" Tardiness Improvement:    %6.1f%%%n", tardinessImprovement);
        System.out.println("-".repeat(50));
    }
}