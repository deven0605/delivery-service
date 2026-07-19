package com.thalicloud.delivery.enums;

// FR-2.2 — mirrors com.thalicloud.auth.enums.VehicleType.
public enum VehicleType {
    BICYCLE,
    BIKE,
    ON_FOOT;

    public boolean isMotorized() {
        return this == BIKE;
    }
}
