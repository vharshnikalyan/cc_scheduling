package com.cloudscheduling.model;

public class VirtualMachine {
    private int vmId;
    private String vmName;
    private int mips;
    private int ram;
    private int bandwidth;
    private int pesNumber;
    private int storage;
    private double availableTime;

    // Constructor
    public VirtualMachine(int vmId, String vmName, int mips, int ram, int bandwidth, int pesNumber, int storage) {
        this.vmId = vmId;
        this.vmName = vmName;
        this.mips = mips;
        this.ram = ram;
        this.bandwidth = bandwidth;
        this.pesNumber = pesNumber;
        this.storage = storage;
        this.availableTime = 0.0;
    }

    // Getters and Setters
    public int getVmId() {
        return vmId;
    }

    public void setVmId(int vmId) {
        this.vmId = vmId;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public int getMips() {
        return mips;
    }

    public void setMips(int mips) {
        this.mips = mips;
    }

    public int getRam() {
        return ram;
    }

    public void setRam(int ram) {
        this.ram = ram;
    }

    public int getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(int bandwidth) {
        this.bandwidth = bandwidth;
    }

    public int getPesNumber() {
        return pesNumber;
    }

    public void setPesNumber(int pesNumber) {
        this.pesNumber = pesNumber;
    }

    public int getStorage() {
        return storage;
    }

    public void setStorage(int storage) {
        this.storage = storage;
    }

    public double getAvailableTime() {
        return availableTime;
    }

    public void setAvailableTime(double availableTime) {
        this.availableTime = availableTime;
    }

    // Utility method to calculate execution time for a job
    public double calculateExecutionTime(int jobLength) {
        return (double) jobLength / mips;
    }

    @Override
    public String toString() {
        return String.format(
            "VirtualMachine{id=%d, name='%s', mips=%d, ram=%d, bandwidth=%d, pes=%d, storage=%d, available=%.2f}",
            vmId, vmName, mips, ram, bandwidth, pesNumber, storage, availableTime
        );
    }
}