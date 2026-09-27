package com.wasteflow.truck;

import com.wasteflow.location.Location;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "trucks")
public class Truck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String truckCode;
    private double capacityKg;
    private double currentLoadKg;

    @Enumerated(EnumType.STRING)
    private TruckStatus status;

    @ManyToOne(optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    protected Truck() {
    }

    public Truck(String truckCode, double capacityKg, Location location) {
        this.truckCode = truckCode;
        this.capacityKg = capacityKg;
        this.location = location;
        this.status = TruckStatus.AVAILABLE;
    }

    public Long getId() { return id; }
    public String getTruckCode() { return truckCode; }
    public double getCapacityKg() { return capacityKg; }
    public double getCurrentLoadKg() { return currentLoadKg; }
    public TruckStatus getStatus() { return status; }
    public Location getLocation() { return location; }
    public void assign(double loadKg) {
        if (loadKg > capacityKg) {
            throw new IllegalArgumentException("Truck capacity exceeded");
        }
        currentLoadKg = loadKg;
        status = TruckStatus.ASSIGNED;
    }
    public void startCollecting() { status = TruckStatus.COLLECTING; }
    public void reduceLoad(double collectedKg) {
        currentLoadKg = Math.max(0, currentLoadKg - collectedKg);
    }
    public void makeAvailable() { currentLoadKg = 0; status = TruckStatus.AVAILABLE; }
}