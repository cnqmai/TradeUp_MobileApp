package com.example.tradeup.model;

import java.time.Instant;

public class Tag {
    private String name;
    private Instant created_at;
    private Instant updated_at;

    public Tag() {}

    public Tag(String name, Instant created_at, Instant updated_at) {
        this.name = name;
        this.created_at = created_at;
        this.updated_at = updated_at;
    }

    public String getName() { return name; }
    public Instant getCreated_at() { return created_at; }
    public Instant getUpdated_at() { return updated_at; }

    public void setName(String name) { this.name = name; }
    public void setCreated_at(Instant created_at) { this.created_at = created_at; }
    public void setUpdated_at(Instant updated_at) { this.updated_at = updated_at; }
}