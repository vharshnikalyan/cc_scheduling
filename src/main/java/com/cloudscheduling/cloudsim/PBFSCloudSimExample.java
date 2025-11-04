package com.cloudscheduling.cloudsim;

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

public class PBFSCloudSimExample {

    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    private Random random = new Random();

    public PBFSCloudSimExample() {
        runSimulation();
    }

    private void runSimulation() {
        System.out.println("🚀 Starting PBFS CloudSim Simulation with 50 Cloudlets...");
        
        // Create CloudSim instance
        CloudSim simulation = new CloudSim();
        
        Datacenter datacenter = createDatacenter(simulation);
        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);
        
        // Store VM and cloudlet lists for later use
        vmList = createVms();
        cloudletList = createCloudlets();
        
        System.out.println("✅ Created " + vmList.size() + " VMs and " + cloudletList.size() + " Cloudlets");
        
        // Apply PBFS scheduling
        applyPBFScheduling(cloudletList, vmList);
        
        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);
        
        System.out.println("⏳ Starting CloudSim simulation...");
        simulation.start();
        
        printResults(broker);
    }

    private void applyPBFScheduling(List<Cloudlet> cloudlets, List<Vm> vms) {
        System.out.println("\n🔧 Applying PBFS Scheduling in CloudSim...");
        
        // Reset any previous VM assignments
        for (Cloudlet cloudlet : cloudlets) {
            cloudlet.setVm(Vm.NULL);
        }
        
        // Create a mapping of cloudlets to their original indices
        List<CloudletInfo> cloudletInfos = new ArrayList<>();
        for (int i = 0; i < cloudlets.size(); i++) {
            cloudletInfos.add(new CloudletInfo(cloudlets.get(i), i));
        }
        
        // Sort cloudlets by length (shorter first) for PBFS-like behavior
        cloudletInfos.sort((c1, c2) -> Long.compare(c1.cloudlet.getLength(), c2.cloudlet.getLength()));
        
        System.out.println("📋 Cloudlet Assignment using PBFS Strategy (First 10):");
        
        // Simple round-robin assignment to ensure all VMs get work
        int vmIndex = 0;
        int displayCount = 0;
        for (CloudletInfo cloudletInfo : cloudletInfos) {
            Cloudlet cloudlet = cloudletInfo.cloudlet;
            int originalIndex = cloudletInfo.originalIndex;
            
            // Assign to next available VM
            Vm assignedVm = vms.get(vmIndex % vms.size());
            cloudlet.setVm(assignedVm);
            
            double executionTime = (double) cloudlet.getLength() / assignedVm.getMips();
            
            // Display first 10 assignments only
            if (displayCount < 10) {
                System.out.printf("  - Cloudlet %d (Length: %d) → VM %d (%d MIPS) | Est. Time: %.2f sec%n", 
                    originalIndex, cloudlet.getLength(), assignedVm.getId(), (int)assignedVm.getMips(), executionTime);
                displayCount++;
            }
            
            vmIndex++;
        }
        if (cloudlets.size() > 10) {
            System.out.printf("  ... and %d more cloudlets assigned%n", cloudlets.size() - 10);
        }
    }

    // Helper class to track original cloudlet indices
    private static class CloudletInfo {
        Cloudlet cloudlet;
        int originalIndex;
        
        CloudletInfo(Cloudlet cloudlet, int originalIndex) {
            this.cloudlet = cloudlet;
            this.originalIndex = originalIndex;
        }
    }

    private Datacenter createDatacenter(CloudSim simulation) {
        List<Host> hostList = new ArrayList<>();
        
        // Create hosts with sufficient resources for all VMs
        for (int i = 0; i < 8; i++) { // 8 hosts for 8 VMs
            List<Pe> peList = new ArrayList<>();
            // Each host has 8 PEs (Processing Elements)
            for (int j = 0; j < 8; j++) {
                peList.add(new PeSimple(4000)); // 4000 MIPS per PE
            }
            
            // Host with ample RAM, BW, and storage
            Host host = new HostSimple(32768, 1000000, 100000, peList); // 32GB RAM
            hostList.add(host);
        }
        
        System.out.println("✅ Created Datacenter with " + hostList.size() + " hosts (" + 
                          hostList.get(0).getPeList().size() + " PEs each)");
        return new DatacenterSimple(simulation, hostList);
    }

    private List<Vm> createVms() {
        List<Vm> vmList = new ArrayList<>();
        
        // Create 8 VMs with balanced resource requirements
        int[] vmMips = {1000, 1000, 2000, 2000, 3000, 3000, 4000, 4000};
        int[] vmPes = {2, 2, 4, 4, 4, 4, 8, 8};
        int[] vmRam = {2048, 2048, 4096, 4096, 8192, 8192, 8192, 8192};
        
        for (int i = 0; i < 8; i++) {
            Vm vm = new VmSimple(vmMips[i], vmPes[i]);
            vm.setRam(vmRam[i]).setBw(1000).setSize(10000);
            vmList.add(vm);
            System.out.printf("  - VM %d: %d MIPS, %d PEs, %d MB RAM%n", 
                i, vmMips[i], vmPes[i], vmRam[i]);
        }
        
        return vmList;
    }

    private List<Cloudlet> createCloudlets() {
        List<Cloudlet> cloudletList = new ArrayList<>();
        
        // Generate 50 cloudlets with varied lengths
        System.out.println("📝 Generating 50 cloudlets with varied lengths...");
        for (int i = 0; i < 50; i++) {
            int length = generateCloudletLength();
            Cloudlet cloudlet = new CloudletSimple(length, 1);
            cloudletList.add(cloudlet);
        }
        
        // Show sample of cloudlet lengths
        System.out.print("  - Cloudlet lengths sample: ");
        for (int i = 0; i < Math.min(10, cloudletList.size()); i++) {
            System.out.print(cloudletList.get(i).getLength() + " ");
        }
        if (cloudletList.size() > 10) {
            System.out.print("...");
        }
        System.out.println();
        
        return cloudletList;
    }

    private int generateCloudletLength() {
        // Cloudlet lengths between 1000 and 20000 MI
        int[] lengths = {1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000, 
                        6000, 7000, 8000, 9000, 10000, 12000, 15000, 18000, 20000};
        return lengths[random.nextInt(lengths.length)];
    }

    private void printResults(DatacenterBroker broker) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("=== CLOUDSIM PBFS SIMULATION RESULTS ===");
        System.out.println("=".repeat(60));
        
        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        List<Cloudlet> allCloudlets = broker.getCloudletSubmittedList();
        
        if (finishedCloudlets.isEmpty()) {
            System.out.println("\n❌ No cloudlets were executed!");
            return;
        }
        
        // Show cloudlet results (first 10 only)
        System.out.println("\n📋 CLOUDLET EXECUTION RESULTS (First 10):");
        List<Cloudlet> displayCloudlets = finishedCloudlets.subList(0, Math.min(10, finishedCloudlets.size()));
        new CloudletsTableBuilder(displayCloudlets).build();
        
        if (finishedCloudlets.size() > 10) {
            System.out.printf("... and %d more cloudlets executed%n", finishedCloudlets.size() - 10);
        }
        
        // Show failed cloudlets if any
        List<Cloudlet> failedCloudlets = new ArrayList<>(allCloudlets);
        failedCloudlets.removeAll(finishedCloudlets);
        
        if (!failedCloudlets.isEmpty()) {
            System.out.println("\n❌ FAILED CLOUDLETS:");
            System.out.println("  - " + failedCloudlets.size() + " cloudlets could not be executed");
        }
        
        // Calculate metrics
        calculateAndDisplayMetrics(broker);
    }

    private void calculateAndDisplayMetrics(DatacenterBroker broker) {
        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        List<Vm> createdVms = broker.getVmCreatedList();
        
        // Calculate makespan (maximum finish time)
        double makespan = finishedCloudlets.stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max().orElse(0);
        
        // Calculate total execution time
        double totalExecutionTime = finishedCloudlets.stream()
                .mapToDouble(Cloudlet::getActualCpuTime)
                .sum();
        
        // Calculate average execution time
        double avgExecutionTime = finishedCloudlets.stream()
                .mapToDouble(Cloudlet::getActualCpuTime)
                .average().orElse(0);
        
        // Calculate total waiting time
        double totalWaitingTime = finishedCloudlets.stream()
                .mapToDouble(cl -> cl.getFinishTime() - cl.getExecStartTime() - cl.getActualCpuTime())
                .sum();
        
        System.out.println("\n🎯 CLOUDSIM PERFORMANCE METRICS:");
        System.out.println("-".repeat(50));
        System.out.printf("⏱️  Makespan Time:           %10.2f seconds%n", makespan);
        System.out.printf("📈 Total Execution Time:     %10.2f seconds%n", totalExecutionTime);
        System.out.printf("⚡ Avg Execution Time:       %10.2f seconds%n", avgExecutionTime);
        System.out.printf("⏳ Total Waiting Time:       %10.2f seconds%n", totalWaitingTime);
        System.out.printf("📦 Completed Cloudlets:      %7d/%d%n", 
            finishedCloudlets.size(), 
            cloudletList.size());
        System.out.printf("🖥️  VMs Created:             %7d/%d%n",
            createdVms.size(),
            vmList.size());
        
        // Show VM utilization
        displayCloudSimVMAssignments(broker, makespan);
    }

    private void displayCloudSimVMAssignments(DatacenterBroker broker, double makespan) {
        List<Cloudlet> finishedCloudlets = broker.getCloudletFinishedList();
        List<Vm> createdVms = broker.getVmCreatedList();
        
        System.out.println("\n🔧 CLOUDSIM VM ASSIGNMENTS:");
        System.out.println("-".repeat(85));
        System.out.printf("%-6s | %-4s | %-8s | %-12s | %-12s | %-10s | %-8s%n", 
            "VM ID", "PEs", "MIPS", "Cloudlets", "Total Time", "Avg Time", "Utilization");
        System.out.println("-".repeat(85));
        
        for (Vm vm : createdVms) {
            List<Cloudlet> vmCloudlets = finishedCloudlets.stream()
                    .filter(cl -> cl.getVm() != null && cl.getVm().equals(vm))
                    .toList();
            
            double totalExecutionTime = vmCloudlets.stream()
                    .mapToDouble(Cloudlet::getActualCpuTime)
                    .sum();
            
            // Calculate actual VM busy time
            double firstStart = vmCloudlets.stream()
                    .mapToDouble(Cloudlet::getExecStartTime)
                    .min().orElse(0);
            double lastFinish = vmCloudlets.stream()
                    .mapToDouble(Cloudlet::getFinishTime)
                    .max().orElse(0);
            double vmBusyTime = lastFinish - firstStart;
            
            double avgTime = vmCloudlets.isEmpty() ? 0 : totalExecutionTime / vmCloudlets.size();
            
            // CORRECT Utilization: (VM busy time / makespan) * 100
            double utilization = makespan > 0 ? Math.min((vmBusyTime / makespan) * 100, 100.0) : 0;
            
            System.out.printf("VM %-3d | %-4d | %-8d | %-10d | %-11.2fs | %-11.2fs | %-8.1f%%%n", 
                vm.getId(), vm.getNumberOfPes(), (int)vm.getMips(), 
                vmCloudlets.size(), totalExecutionTime, avgTime, utilization);
        }
        
        // Show failed VMs if any
        List<Vm> failedVms = new ArrayList<>(vmList);
        failedVms.removeAll(createdVms);
        
        if (!failedVms.isEmpty()) {
            System.out.println("\n❌ FAILED VMs (not allocated):");
            for (Vm vm : failedVms) {
                System.out.printf("  - VM %d: %d MIPS, %d PEs, %d MB RAM%n",
                    vm.getId(), (int)vm.getMips(), vm.getNumberOfPes(), vm.getRam().getCapacity());
            }
        }
    }
    
    // Main method for standalone execution
    public static void main(String[] args) {
        new PBFSCloudSimExample();
    }
}