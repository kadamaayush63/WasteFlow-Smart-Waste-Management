package com.wasteflow.truck;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TruckRepository extends JpaRepository<Truck, Long> {
    List<Truck> findByStatusOrderById(TruckStatus status);
}