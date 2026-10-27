package com.wasteflow.route;

import com.wasteflow.truck.Truck;
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
@Table(name = "routes")
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "truck_id", nullable = false)
    private Truck truck;

    private String algorithm;

    @Enumerated(EnumType.STRING)
    private RouteStatus status;

    private double totalDistance;
    private double totalTime;
    private Instant createdAt;
    private Instant completedAt;

    protected Route() {
    }

    public Route(Truck truck, String algorithm, double totalDistance, double totalTime) {
        this.truck = truck;
        this.algorithm = algorithm;
        this.status = RouteStatus.ASSIGNED;
        this.totalDistance = totalDistance;
        this.totalTime = totalTime;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Truck getTruck() { return truck; }
    public String getAlgorithm() { return algorithm; }
    public RouteStatus getStatus() { return status; }
    public double getTotalDistance() { return totalDistance; }
    public double getTotalTime() { return totalTime; }
    public Instant getCreatedAt() { return createdAt; }
    public void start() { status = RouteStatus.COLLECTING; }
    public void complete() { status = RouteStatus.COMPLETED; completedAt = Instant.now(); }
    public void cancel() { status = RouteStatus.CANCELLED; }
}