package com.wasteflow.collection;

import com.wasteflow.bin.WasteBin;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "collection_requests")
public class CollectionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bin_id", nullable = false)
    private WasteBin bin;

    private double priorityScore;
    private String urgency;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private Instant createdAt;
    private Instant resolvedAt;

    protected CollectionRequest() {
    }

    public CollectionRequest(WasteBin bin) {
        this.bin = bin;
        this.priorityScore = bin.getCurrentFillKg() / bin.getCapacityKg() * 100;
        this.urgency = bin.getStatus().name();
        this.status = RequestStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public WasteBin getBin() { return bin; }
    public double getPriorityScore() { return priorityScore; }
    public String getUrgency() { return urgency; }
    public RequestStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void assign() { status = RequestStatus.ASSIGNED; }
    public void complete() { status = RequestStatus.COMPLETED; resolvedAt = Instant.now(); }
    public void cancel() { status = RequestStatus.CANCELLED; resolvedAt = Instant.now(); }

    public void refreshFromBin() {
        priorityScore = bin.getCurrentFillKg() / bin.getCapacityKg() * 100;
        urgency = bin.getStatus().name();
    }
}