package com.evogroup.minicrm.dto;

import java.time.Instant;

public class AuditLogResponse {

    private Long id;
    private String who;
    private String action;
    private String entity;
    private Long entityId;
    private Instant timestamp;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWho() { return who; }
    public void setWho(String who) { this.who = who; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
