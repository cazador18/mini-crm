package com.evogroup.minicrm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String who;

    @NotBlank
    private String action;

    @NotBlank
    private String entity;

    @NotNull
    @Column(name = "entity_id")
    private Long entityId;

    @NotNull
    private Instant timestamp = Instant.now();

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
