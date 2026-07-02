package com.evogroup.minicrm.dto;

import com.evogroup.minicrm.model.TaskStatus;

import java.util.Map;

public class DashboardResponse {

    private long totalClients;
    private Map<TaskStatus, Long> tasksByStatus;

    public long getTotalClients() { return totalClients; }
    public void setTotalClients(long totalClients) { this.totalClients = totalClients; }

    public Map<TaskStatus, Long> getTasksByStatus() { return tasksByStatus; }
    public void setTasksByStatus(Map<TaskStatus, Long> tasksByStatus) { this.tasksByStatus = tasksByStatus; }
}
