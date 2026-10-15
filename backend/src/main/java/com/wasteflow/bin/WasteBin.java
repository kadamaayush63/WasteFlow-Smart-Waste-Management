package com.wasteflow.bin;

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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "waste_bins", uniqueConstraints = @UniqueConstraint(name = "uk_waste_bins_code", columnNames = "bin_code"))
public class WasteBin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String binCode;

    @ManyToOne(optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    private double capacityKg;
    private double currentFillKg;

    @Enumerated(EnumType.STRING)
    private WasteType wasteType;

    @Enumerated(EnumType.STRING)
    private BinStatus status;

    private Instant lastCollectedAt;
    private Instant createdAt;
    private Instant updatedAt;

    protected WasteBin() {
    }

    public WasteBin(String binCode, Location location, double capacityKg, double currentFillKg, WasteType wasteType) {
        this.binCode = binCode;
        this.location = location;
        this.capacityKg = capacityKg;
        this.currentFillKg = currentFillKg;
        this.wasteType = wasteType;
        this.status = classifyStatus();
    }

    public Long getId() { return id; }
    public String getBinCode() { return binCode; }
    public Location getLocation() { return location; }
    public double getCapacityKg() { return capacityKg; }
    public double getCurrentFillKg() { return currentFillKg; }
    public WasteType getWasteType() { return wasteType; }
    public BinStatus getStatus() { return status; }
    public Instant getLastCollectedAt() { return lastCollectedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(Location location, double capacityKg, double currentFillKg, WasteType wasteType) {
        this.location = location;
        this.capacityKg = capacityKg;
        this.currentFillKg = currentFillKg;
        this.wasteType = wasteType;
        this.status = classifyStatus();
    }

    public void setCurrentFillKg(double currentFillKg) {
        if (!Double.isFinite(currentFillKg) || currentFillKg < 0 || currentFillKg > capacityKg) {
            throw new IllegalArgumentException("Current fill must be between 0 and bin capacity");
        }
        this.currentFillKg = currentFillKg;
        this.status = classifyStatus();
    }

    public BinStatus classifyStatus() {
        double fillRatio = capacityKg == 0 ? 0 : currentFillKg / capacityKg;
        if (fillRatio >= 0.95) return BinStatus.CRITICAL;
        if (fillRatio >= 0.75) return BinStatus.HIGH;
        if (fillRatio >= 0.50) return BinStatus.MEDIUM;
        return BinStatus.NORMAL;
    }

    public void increaseFill(double amount) {
        currentFillKg = Math.min(capacityKg, currentFillKg + amount);
        status = classifyStatus();
    }

    public void collect() {
        currentFillKg = 0;
        status = BinStatus.NORMAL;
        lastCollectedAt = Instant.now();
    }
}