package com.wasteflow.collection;

import com.wasteflow.bin.WasteBin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionRequestRepository extends JpaRepository<CollectionRequest, Long> {
    boolean existsByBinAndStatusIn(WasteBin bin, List<RequestStatus> statuses);
    List<CollectionRequest> findByStatusOrderByPriorityScoreDescCreatedAtAsc(RequestStatus status);
    Optional<CollectionRequest> findFirstByBinAndStatusInOrderByCreatedAtDesc(WasteBin bin, List<RequestStatus> statuses);
}