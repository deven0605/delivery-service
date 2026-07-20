package com.thalicloud.delivery.dto.response;

import com.thalicloud.delivery.entity.DeliveryPartner;
import com.thalicloud.delivery.enums.DutyStatus;
import com.thalicloud.delivery.enums.PartnerLifecycleState;
import com.thalicloud.delivery.enums.VehicleType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class PartnerProfileResponse {
    private final UUID id;
    private final String phone;
    private final String name;
    private final LocalDate dob;
    private final String gender;
    private final String email;
    private final String profilePhotoUrl;
    private final VehicleType vehicleType;
    private final String vehicleNumber;
    private final String vehicleModel;
    private final String bankAccountHolderName;
    private final String bankAccountNumber;
    private final String bankIfscCode;
    private final String upiId;
    private final PartnerLifecycleState lifecycleState;
    private final boolean registrationComplete;
    private final DutyStatus dutyStatus;
    private final Double rating;
    private final List<DocumentResponse> documents;

    public static PartnerProfileResponse from(DeliveryPartner partner, List<DocumentResponse> documents) {
        return PartnerProfileResponse.builder()
                .id(partner.getId())
                .phone(partner.getPhone())
                .name(partner.getName())
                .dob(partner.getDob())
                .gender(partner.getGender())
                .email(partner.getEmail())
                .profilePhotoUrl(partner.getProfilePhotoUrl())
                .vehicleType(partner.getVehicleType())
                .vehicleNumber(partner.getVehicleNumber())
                .vehicleModel(partner.getVehicleModel())
                .bankAccountHolderName(partner.getBankAccountHolderName())
                .bankAccountNumber(partner.getBankAccountNumber())
                .bankIfscCode(partner.getBankIfscCode())
                .upiId(partner.getUpiId())
                .lifecycleState(partner.getLifecycleState())
                .registrationComplete(partner.isRegistrationComplete())
                .dutyStatus(partner.getDutyStatus())
                .rating(partner.getRating())
                .documents(documents)
                .build();
    }
}
