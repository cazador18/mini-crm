package com.evogroup.minicrm.exception;

import com.evogroup.minicrm.model.TaskStatus;

public class InvalidTaskStatusTransitionException extends RuntimeException {
    public InvalidTaskStatusTransitionException(TaskStatus from, TaskStatus to) {
        super("Cannot transition task status from " + from + " to " + to);
    }
}
