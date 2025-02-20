package com.netflix.conductor.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "meta_event_handler")
public class MetaEventHandler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_on", updatable = false, nullable = false)
    private Instant createdOn = Instant.now();

    @Column(name = "modified_on", nullable = false)
    private Instant modifiedOn = Instant.now();

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "event", nullable = false)
    private String event;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "json_data", nullable = false, columnDefinition = "TEXT")
    private String jsonData;
}
