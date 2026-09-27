package com.wasteflow.location;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Entity
@Table(name = "locations")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotNull
    private Double xCoordinate;

    @NotNull
    private Double yCoordinate;

    private Instant createdAt;

    protected Location() {
    }

    public Location(String name, Double xCoordinate, Double yCoordinate) {
        this.name = name;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Double getXCoordinate() { return xCoordinate; }
    public Double getYCoordinate() { return yCoordinate; }
    public Instant getCreatedAt() { return createdAt; }

    public void update(String name, Double xCoordinate, Double yCoordinate) {
        this.name = name;
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
    }
}