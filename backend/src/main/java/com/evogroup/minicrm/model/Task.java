package com.evogroup.minicrm.model;

import jakarta.persistence.*;
import java.time.LocalDate;

// TODO(Claude Code): добавить @ManyToOne Client, валидацию, getters/setters,
// возможно @Enumerated(EnumType.STRING) для status/priority.
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.NEW;

    @Enumerated(EnumType.STRING)
    private TaskPriority priority = TaskPriority.MEDIUM;

    private LocalDate deadline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    // getters/setters — TODO
}
