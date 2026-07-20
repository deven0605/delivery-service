package com.thalicloud.delivery.dto.request;

import lombok.Getter;
import lombok.Setter;

// FR-2.8 — either the bank trio (accountHolderName/accountNumber/ifscCode) or
// upiId is sufficient; the "at least one method" rule is enforced in the
// service layer, not here (bean validation can't express an either/or cleanly).
@Getter
@Setter
public class BankDetailsRequest {

    private String accountHolderName;

    private String accountNumber;

    private String ifscCode;

    private String upiId;
}
