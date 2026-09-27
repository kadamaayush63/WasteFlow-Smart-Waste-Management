package com.wasteflow.bin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WasteBinRepository extends JpaRepository<WasteBin, Long> {
    Optional<WasteBin> findByBinCode(String binCode);
    List<WasteBin> findAllByOrderByStatusDescCurrentFillKgDesc();
}