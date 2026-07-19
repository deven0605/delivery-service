package com.thalicloud.delivery.dto.request;

import com.thalicloud.delivery.enums.VehicleType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// FR-2.2/FR-2.3 — cross-field requirement (vehicleNumber mandatory when
// motorized) is enforced in the service layer, not here.
@Getter
@Setter
public class VehicleDetailsRequest {

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    private String vehicleNumber;

    private String vehicleModel;
}
