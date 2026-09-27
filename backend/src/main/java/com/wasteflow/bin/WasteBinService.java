package com.wasteflow.bin;

import com.wasteflow.location.Location;
import com.wasteflow.location.LocationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WasteBinService {

    private final WasteBinRepository binRepository;
    private final LocationRepository locationRepository;

    public WasteBinService(WasteBinRepository binRepository, LocationRepository locationRepository) {
        this.binRepository = binRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<WasteBinResponse> findAll() {
        return binRepository.findAllByOrderByStatusDescCurrentFillKgDesc()
                .stream()
                .map(WasteBinResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WasteBinResponse findById(Long id) {
        return WasteBinResponse.from(getBin(id));
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