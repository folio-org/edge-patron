package org.folio.edge.patron.model;

import java.io.IOException;
import java.util.UUID;

public class HoldCancellationValidator {
  public static String validateCancelHoldRequest(String holdCancellationRequest) {
    String errorMessage = null;
    try {
      final HoldCancellation holdCancellation = HoldCancellation.fromJson(holdCancellationRequest);
      if (validateRequiredHoldCancellationFields(holdCancellation)) {
        errorMessage = "required fields for cancelling holds are missing (holdId, cancellationReasonId, canceledByUserId)";
      } else if (areRequiredHoldCancellationFieldsUUIDs(holdCancellation)) {
        errorMessage = "invalid values for one of the required fields (holdId, cancellationReasonId, canceledByUserId)";
      }
    } catch (Exception e) {
      errorMessage = "invalid holdCancellationRequest";
    }
    return errorMessage;
  }

  private static boolean validateRequiredHoldCancellationFields(HoldCancellation holdCancellation) {
    return (isNullOrEmpty(holdCancellation.holdId) ||
      isNullOrEmpty(holdCancellation.cancellationReasonId)||
      isNullOrEmpty(holdCancellation.canceledByUserId)
    );
  }

  private static boolean areRequiredHoldCancellationFieldsUUIDs(HoldCancellation holdCancellation) {
    return (!isUUID (holdCancellation.holdId) ||
      !isUUID (holdCancellation.cancellationReasonId) ||
      !isUUID (holdCancellation.canceledByUserId)
    );
  }

  private static boolean isNullOrEmpty(String stringToCheck) {
    return stringToCheck == null || stringToCheck.isEmpty();
  }

  private static boolean isUUID(String uuidString) {
    try {
      return UUID.fromString(uuidString) != null;
    } catch (Exception ex) {
      return false;
    }
  }
}
