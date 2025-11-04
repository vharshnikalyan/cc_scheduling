package com.cloudscheduling.sim;

import com.cloudscheduling.dao.JobDAO;
import com.cloudscheduling.model.CloudJob;
import org.cloudbus.cloudsim.allocationpolicies.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.utilizationmodels.UtilizationModelFull; // ✅ Use this instead of UtilizationModelNull

import java.util.*;
import java.util.stream.Collectors;

public class SimulationUtils {

    // ---------- Hosts ----------
    public static Host createHost(long mips, int pes) {
        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < pes; i++) {
            peList.add(new PeSimple(mips));
        }
        return new HostSimple(16384, 10000, 1_000_000, peList);
    }

    public static List<Host> createVariedHosts() {
        List<Host> hosts = new ArrayList<>();
        hosts.add(createHost(800, 2));
        hosts.add(createHost(1000, 2));
        hosts.add(createHost(1200, 2));
        hosts.add(createHost(1500, 2));
        return hosts;
    }

    public static DatacenterSimple createDatacenter(CloudSim sim, List<Host> hosts) {
        return new DatacenterSimple(sim, hosts, new VmAllocationPolicySimple());
    }

    // ---------- VM creation helpers ----------
    public static List<Vm> createVmList(int count, long baseMips, int pes, long ram, long bw, long size) {
        List<Vm> vms = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Vm vm = new VmSimple(baseMips, pes);
            vm.setRam((int) ram);
            vm.setBw(bw);
            vm.setSize(size);
            vms.add(vm);
        }
        return vms;
    }

    public static List<Vm> createVmList(List<Long> mipsList, int pes, long ram, long bw, long size) {
        List<Vm> vms = new ArrayList<>();
        for (Long mips : mipsList) {
            Vm vm = new VmSimple(mips, pes);
            vm.setRam((int) ram);
            vm.setBw(bw);
            vm.setSize(size);
            vms.add(vm);
        }
        return vms;
    }

    public static List<Vm> createVmList() {
        List<Vm> vms = new ArrayList<>();

        Vm vm0 = new VmSimple(800, 2);
        vm0.setRam(2048);
        vm0.setBw(1000);
        vm0.setSize(10_000);
        vms.add(vm0);

        Vm vm1 = new VmSimple(1000, 2);
        vm1.setRam(2048);
        vm1.setBw(1000);
        vm1.setSize(10_000);
        vms.add(vm1);

        Vm vm2 = new VmSimple(1200, 2);
        vm2.setRam(2048);
        vm2.setBw(1000);
        vm2.setSize(10_000);
        vms.add(vm2);

        Vm vm3 = new VmSimple(1500, 2);
        vm3.setRam(2048);
        vm3.setBw(1000);
        vm3.setSize(10_000);
        vms.add(vm3);

        return vms;
    }

    // ---------- Cloudlet helpers ----------
    public static List<Cloudlet> createCloudletsFromJobs(List<CloudJob> jobs) {
        List<Cloudlet> cloudlets = new ArrayList<>();
        for (CloudJob job : jobs) {
            // ✅ Use UtilizationModelFull to make cloudlet use full CPU until finish
            CloudletSimple cl = new CloudletSimple(job.getJobLength(), 1, new UtilizationModelFull());
            cloudlets.add(cl);
        }
        return cloudlets;
    }

    public static Map<Integer, Integer> buildCloudletIdToJobIdMap(List<Cloudlet> cloudlets, List<CloudJob> jobs) {
        Map<Integer, Integer> map = new HashMap<>();
        int n = Math.min(cloudlets.size(), jobs.size());
        for (int i = 0; i < n; i++) {
            Cloudlet cl = cloudlets.get(i);
            map.put((int) cl.getId(), jobs.get(i).getJobId());
        }
        return map;
    }

   public static Map<Integer, Double> collectCompletionTimes(List<Cloudlet> finishedCloudlets,
                                                          Map<Integer, Integer> cloudletIdToJobId) {
    Map<Integer, Double> completionTimes = new HashMap<>();
    if (finishedCloudlets == null || cloudletIdToJobId == null) return completionTimes;

    for (Cloudlet cl : finishedCloudlets) {
        // Avoid relying on enum names (cloudsim versions differ).
        // If a cloudlet finished successfully it will have a positive finish time.
        double finish = cl.getFinishTime();
        if (finish > 0) {
            Integer jobId = cloudletIdToJobId.get((int) cl.getId());
            if (jobId != null) {
                completionTimes.put(jobId, finish);
            }
        }
    }
    return completionTimes;
}


    public static void saveCompletionTimesToDb(JobDAO jobDAO, Map<Integer, Double> completionTimes) {
        if (jobDAO == null || completionTimes == null || completionTimes.isEmpty()) {
            if (jobDAO == null) System.err.println("❌ JobDAO is null, cannot save completion times.");
            else System.out.println("⚠️ No completion times to save.");
            return;
        }
        jobDAO.updateJobCompletionTimes(completionTimes);
    }

    public static List<Cloudlet> getFinishedCloudlets(DatacenterBrokerSimple broker) {
        return broker.getCloudletFinishedList().stream().collect(Collectors.toList());
    }
    public static DatacenterSimple createDatacenter(CloudSim sim) {
        List<Host> hostList = new ArrayList<>();

        hostList.add(createHost(800, 2));   // Host 0 - slow
        hostList.add(createHost(1000, 2));  // Host 1 - medium
        hostList.add(createHost(1200, 2));  // Host 2 - fast
        hostList.add(createHost(1500, 2));  // Host 3 - very fast

        DatacenterSimple datacenter = new DatacenterSimple(sim, hostList);
        datacenter.setName("DatacenterSimple1");
        return datacenter;
    }

    private static Host createHost(int mipsPerPe, int peCount) {
        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < peCount; i++) {
            peList.add(new PeSimple(mipsPerPe));
        }
        Host host = new HostSimple(8192, 10000, 1_000_000, peList);
        host.setVmScheduler(new VmSchedulerTimeShared());
        return host;
    }
}

