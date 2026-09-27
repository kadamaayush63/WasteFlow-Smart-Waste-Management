package com.wasteflow.collection;

public record CollectionRequestResponse(Long id, Long binId, String binCode, double priorityScore, String urgency, RequestStatus status) {
    public static CollectionRequestResponse from(CollectionRequest request) {
        return new CollectionRequestResponse(
                request.getId(), request.getBin().getId(), request.getBin().getBinCode(),
                request.getPriorityScore(), request.getUrgency(), request.getStatus()
        );
    }
}