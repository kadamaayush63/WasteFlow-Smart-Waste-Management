package com.wasteflow.bin;

import com.wasteflow.location.Location;
import com.wasteflow.location.LocationRepository;
import com.wasteflow.collection.CollectionRequest;
import com.wasteflow.collection.CollectionRequestRepository;
import com.wasteflow.collection.RequestStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class WasteBinService {

    private static final Duration SENSOR_COOLDOWN_AFTER_COLLECTION = Duration.ofSeconds(20);
    private static final double MINIMUM_REQUEST_FILL_PERCENT = 10;
    private static final List<RequestStatus> ACTIVE_REQUEST_STATUSES =
            List.of(RequestStatus.PENDING, RequestStatus.ASSIGNED);

    private final WasteBinRepository binRepository;
    private final LocationRepository locationRepository;
    private final CollectionRequestRepository requestRepository;

    public WasteBinService(
            WasteBinRepository binRepository,
            LocationRepository locationRepository,
            CollectionRequestRepository requestRepository
    ) {
        this.binRepository = binRepository;
        this.locationRepository = locationRepository;
        this.requestRepository = requestRepository;
    }

    @Transactional(readOnly = true)
    public List<WasteBinResponse> findAll(String search) {
        WasteBinResponse[] bins = binRepository.findAllByOrderByStatusDescCurrentFillKgDesc().stream()
                .map(WasteBinResponse::from)
                .toArray(WasteBinResponse[]::new);

        if (search == null || search.isBlank()) {
            return Arrays.asList(bins);
        }

        String query = search.trim().toLowerCase(Locale.ROOT);
        List<WasteBinResponse> matches = new ArrayList<>();
        for (WasteBinResponse bin : bins) {
            if (bin.binCode().toLowerCase(Locale.ROOT).contains(query)
                    || bin.locationName().toLowerCase(Locale.ROOT).contains(query)) {
                matches.add(bin);
            }
        }
        return matches;
    }

    @Transactional(readOnly = true)
    public WasteBinResponse findById(Long id) {
        return WasteBinResponse.from(getBin(id));
    }

    @Transactional
    public void updateFillPercentages(
            Map<Long, Double> fillPercentages,
            Instant readingCreatedAt,
            Instant polledAt
    ) {
        if (fillPercentages.isEmpty()) {
            throw new IllegalArgumentException("At least one bin fill percentage is required");
        }
        if (readingCreatedAt == null || polledAt == null) {
            throw new IllegalArgumentException("ThingSpeak reading and poll timestamps are required");
        }
        for (Map.Entry<Long, Double> entry : fillPercentages.entrySet()) {
            Double percentage = entry.getValue();
            if (entry.getKey() == null || percentage == null
                    || !Double.isFinite(percentage) || percentage < 0 || percentage > 100) {
                throw new IllegalArgumentException("Bin fill percentages must be between 0 and 100");
            }
        }

        Map<Long, WasteBin> binsById = new HashMap<>();
        binRepository.findAllById(fillPercentages.keySet())
                .forEach(bin -> binsById.put(bin.getId(), bin));
        if (binsById.size() != fillPercentages.size()) {
            List<Long> missingBinIds = fillPercentages.keySet().stream()
                    .filter(id -> !binsById.containsKey(id))
                    .toList();
            throw new IllegalStateException("Sensor bins not found: " + missingBinIds);
        }

        fillPercentages.forEach((binId, percentage) -> {
            WasteBin bin = binsById.get(binId);
            Instant lastCollectedAt = bin.getLastCollectedAt();
            if (lastCollectedAt != null
                    && (polledAt.isBefore(lastCollectedAt.plus(SENSOR_COOLDOWN_AFTER_COLLECTION))
                    || !readingCreatedAt.isAfter(lastCollectedAt))) {
                return;
            }
            bin.setCurrentFillKg(bin.getCapacityKg() * percentage / 100);
            var activeRequest = requestRepository.findFirstByBinAndStatusInOrderByCreatedAtDesc(
                    bin, ACTIVE_REQUEST_STATUSES);
            if (activeRequest.isPresent()) {
                CollectionRequest request = activeRequest.get();
                if (percentage == 0 && request.getStatus() == RequestStatus.PENDING) {
                    request.cancel();
                } else {
                    request.refreshFromBin();
                }
            } else if (percentage >= MINIMUM_REQUEST_FILL_PERCENT) {
                requestRepository.save(new CollectionRequest(bin));
            }
        });
    }

    @Transactional
    public WasteBinResponse create(CreateBinRequest request) {
        validateFill(request.capacityKg(), request.currentFillKg());
        if (binRepository.findByBinCode(request.binCode()).isPresent()) {
            throw new IllegalArgumentException("A bin with code " + request.binCode() + " already exists");
        }
        Location location = getLocation(request.locationId());
        return WasteBinResponse.from(binRepository.save(new WasteBin(
                request.binCode(), location, request.capacityKg(), request.currentFillKg(), request.wasteType()
        )));
    }

    @Transactional
    public WasteBinResponse update(Long id, CreateBinRequest request) {
        validateFill(request.capacityKg(), request.currentFillKg());
        WasteBin bin = getBin(id);
        Location location = getLocation(request.locationId());
        bin.update(location, request.capacityKg(), request.currentFillKg(), request.wasteType());
        return WasteBinResponse.from(binRepository.save(bin));
    }

    @Transactional
    public void delete(Long id) {
        try {
            binRepository.delete(getBin(id));
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException("Bin cannot be deleted because it is used by a route or request");
        }
    }

    private WasteBin getBin(Long id) {
        return binRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bin not found: " + id));
    }

    private Location getLocation(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + id));
    }

    private void validateFill(double capacityKg, double currentFillKg) {
        if (currentFillKg > capacityKg) {
            throw new IllegalArgumentException("Current fill cannot exceed bin capacity");
        }
    }

}