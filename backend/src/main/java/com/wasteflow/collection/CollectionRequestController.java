package com.wasteflow.collection;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/collection-requests")
public class CollectionRequestController {

    private final CollectionRequestRepository requestRepository;

    public CollectionRequestController(CollectionRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    @GetMapping
    public List<CollectionRequestResponse> findByStatus(
            @RequestParam(defaultValue = "PENDING") RequestStatus status
    ) {
        return requestRepository.findByStatusOrderByPriorityScoreDescCreatedAtAsc(status)
                .stream().map(CollectionRequestResponse::from).toList();
    }
}