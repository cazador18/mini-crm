package com.evogroup.minicrm.model;

import jakarta.persistence.*;
import java.time.Instant;

// TODO(Claude Code): добавить поля name/email/phone, валидацию (@NotBlank, @Email),
// связь @OneToMany с Task, builder/lombok при желании.
@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    private String phone;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    // getters/setters — TODO
}
