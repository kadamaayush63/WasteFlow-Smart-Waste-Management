package com.wasteflow.simulation;

import com.wasteflow.bin.BinStatus;
import com.wasteflow.bin.WasteBin;
import com.wasteflow.bin.WasteBinRepository;
import com.wasteflow.collection.CollectionRequest;
import com.wasteflow.collection.CollectionRequestRepository;
import com.wasteflow.collection.RequestStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SimulationService {

    private final WasteBinRepository binRepository;
    private final CollectionRequestRepository requestRepository;
    private final double fillIncreaseKg;

    public SimulationService(
            WasteBinRepository binRepository,
            CollectionRequestRepository requestRepository,
            @Value("${app.simulation.fill-increase-kg-per-tick:5}") double fillIncreaseKg
    ) {
        this.binRepository = binRepository;
        this.requestRepository = requestRepository;
        this.fillIncreaseKg = fillIncreaseKg;
    }

    @Transactional
    public SimulationTickResponse tick() {
        List<WasteBin> changedBins = binRepository.findAll().stream()
                .filter(bin -> bin.getStatus() != BinStatus.COLLECTED)
                .peek(bin -> bin.increaseFill(fillIncreaseKg))
                .toList();

        List<CollectionRequest> requests = changedBins.stream()
                .filter(bin -> bin.getStatus() == BinStatus.HIGH || bin.getStatus() == BinStatus.CRITICAL)
                .filter(bin -> !requestRepository.existsByBinAndStatusIn(
                        bin, List.of(RequestStatus.PENDING, RequestStatus.ASSIGNED)))
                .map(CollectionRequest::new)
                .map(requestRepository::save)
                .toList();

        binRepository.saveAll(changedBins);
        return new SimulationTickResponse(changedBins.size(), requests.size());
    }

    public record SimulationTickResponse(int changedBinCount, int createdRequestCount) {
    }
}