package com.rama.mudstock.model.analyst;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "firm")
public class Firm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "benzinga_firm_id", nullable = false, length = 64)
    private String benzingaFirmId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "currency", length = 32)
    private String currency;

    @Column(name = "last_updated")
    private LocalDate lastUpdated;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Firm() {}

    public Firm(String benzingaFirmId, String name, String currency, LocalDate lastUpdated) {
        this.benzingaFirmId = benzingaFirmId;
        this.name = name;
        this.currency = currency;
        this.lastUpdated = lastUpdated;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBenzingaFirmId() { return benzingaFirmId; }
    public void setBenzingaFirmId(String benzingaFirmId) { this.benzingaFirmId = benzingaFirmId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDate lastUpdated) { this.lastUpdated = lastUpdated; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public String toString() {
        return "Firm{id=" + id + ", benzingaFirmId='" + benzingaFirmId + "', name='" + name + "'}";
    }
}