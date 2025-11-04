package com.cloudscheduling.model;

public class CloudJob {
    private int jobId;
    private String jobName;
    private long jobLength;
    private int priority;
    private double arrivalTime;
    private double dueDate;
    private Integer assignedVmId;
    private Double startTime;
    private Double finishTime;
    private Double waitingTime;
    private Double cpuTime;

    // Default constructor (required for your getAllJobs method)
    public CloudJob() {
    }

    // Your existing constructor (keep this for backward compatibility)
    public CloudJob(int jobId, String jobName, long jobLength, int priority, 
                   Double arrivalTime, Double dueDate, Double finishTime) {
        this.jobId = jobId;
        this.jobName = jobName;
        this.jobLength = jobLength;
        this.priority = priority;
        this.arrivalTime = arrivalTime != null ? arrivalTime : 0.0;
        this.dueDate = dueDate != null ? dueDate : 0.0;
        this.finishTime = finishTime;
    }

    // Getters and Setters for all fields
    public int getJobId() {
        return jobId;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public long getJobLength() {
        return jobLength;
    }

    public void setJobLength(long jobLength) {
        this.jobLength = jobLength;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(double arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public double getDueDate() {
        return dueDate;
    }

    public void setDueDate(double dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getAssignedVmId() {
        return assignedVmId;
    }

    public void setAssignedVmId(Integer assignedVmId) {
        this.assignedVmId = assignedVmId;
    }

    public Double getStartTime() {
        return startTime;
    }

    public void setStartTime(Double startTime) {
        this.startTime = startTime;
    }

    public Double getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Double finishTime) {
        this.finishTime = finishTime;
    }

    public Double getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(Double waitingTime) {
        this.waitingTime = waitingTime;
    }

    public Double getCpuTime() {
        return cpuTime;
    }

    public void setCpuTime(Double cpuTime) {
        this.cpuTime = cpuTime;
    }

    @Override
    public String toString() {
        return "CloudJob{" +
                "jobId=" + jobId +
                ", jobName='" + jobName + '\'' +
                ", jobLength=" + jobLength +
                ", priority=" + priority +
                ", arrivalTime=" + arrivalTime +
                ", dueDate=" + dueDate +
                ", finishTime=" + finishTime +
                '}';
    }
}