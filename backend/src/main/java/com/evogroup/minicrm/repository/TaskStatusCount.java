package com.evogroup.minicrm.repository;

import com.evogroup.minicrm.model.TaskStatus;

public interface TaskStatusCount {
    TaskStatus getStatus();
    Long getCount();
}
