package com.wasteflow.route;

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
@Table(name = "route_stops")
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bin_id", nullable = false)
    private WasteBin bin;

    private int sequenceNumber;

    @Enumerated(EnumType.STRING)
    private RouteStopStatus status;

    private Double collectedKg;
    private Instant collectedAt;

    protected RouteStop() {
    }

    public RouteStop(Route route, WasteBin bin, int sequenceNumber) {
        this.route = route;
        this.bin = bin;
        this.sequenceNumber = sequenceNumber;
        this.status = RouteStopStatus.PENDING;
    }

    public Long getId() { return id; }
    public Route getRoute() { return route; }
    public WasteBin getBin() { return bin; }
    public int getSequenceNumber() { return sequenceNumber; }
    public RouteStopStatus getStatus() { return status; }
    public Double getCollectedKg() { return collectedKg; }
    public void complete() {
        collectedKg = bin.getCurrentFillKg();
        collectedAt = Instant.now();
        status = RouteStopStatus.COMPLETED;
        bin.collect();
    }
}