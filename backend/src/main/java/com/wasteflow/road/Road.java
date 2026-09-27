package com.wasteflow.road;

import com.wasteflow.location.Location;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "roads")
public class Road {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "source_location_id", nullable = false)
    private Location source;

    @ManyToOne(optional = false)
    @JoinColumn(name = "destination_location_id", nullable = false)
    private Location destination;

    private double distance;
    private double travelTimeMinutes;
    private boolean bidirectional;

    protected Road() {
    }

    public Road(Location source, Location destination, double distance, double travelTimeMinutes, boolean bidirectional) {
        this.source = source;
        this.destination = destination;
        this.distance = distance;
        this.travelTimeMinutes = travelTimeMinutes;
        this.bidirectional = bidirectional;
    }

    public Long getId() { return id; }
    public Location getSource() { return source; }
    public Location getDestination() { return destination; }
    public double getDistance() { return distance; }
    public double getTravelTimeMinutes() { return travelTimeMinutes; }
    public boolean isBidirectional() { return bidirectional; }
}