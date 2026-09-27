package com.wasteflow.bin;

public record WasteBinResponse(
        Long id,
        String binCode,
        Long locationId,
        String locationName,
        double capacityKg,
        double currentFillKg,
        WasteType wasteType,
        BinStatus status
) {
    public static WasteBinResponse from(WasteBin bin) {
        return new WasteBinResponse(
                bin.getId(),
                bin.getBinCode(),
                bin.getLocation().getId(),
                bin.getLocation().getName(),
                bin.getCapacityKg(),
                bin.getCurrentFillKg(),
                bin.getWasteType(),
                bin.getStatus()
        );
    }
}