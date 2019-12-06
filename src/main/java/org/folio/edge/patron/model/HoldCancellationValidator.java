package org.folio.edge.patron.model;

import io.vertx.core.json.JsonObject;

import java.util.UUID;

import static org.folio.edge.patron.Constants.FIELD_CANCELED_BY_USER_ID;
import static org.folio.edge.patron.Constants.FIELD_CANCELLATION_REASON_ID;
import static org.folio.edge.patron.Constants.FIELD_HOLD_ID;

public class HoldCancellationValidator {

  private HoldCancellationValidator() {}

  public static String validateCancelHoldRequest(JsonObject holdCancellationRequest) {
    String errorMessage = null;
    try {
      if (validateRequiredHoldCancellationFields(holdCancellationRequest)) {
        errorMessage = "required fields for cancelling holds are missing (holdId, cancellationReasonId, canceledByUserId)";
      } else if (areRequiredHoldCancellationFieldsUUIDs(holdCancellationRequest)) {
        errorMessage = "invalid values for one of the required fields (holdId, cancellationReasonId, canceledByUserId)";
      }
    } catch (Exception e) {
      errorMessage = "invalid holdCancellationRequest. " + e.getMessage();
    }
    return errorMessage;
  }

  private static boolean validateRequiredHoldCancellationFields(JsonObject holdCancellation) {
    return (isNullOrEmpty(holdCancellation.getString(FIELD_HOLD_ID)) ||
      isNullOrEmpty(holdCancellation.getString(FIELD_CANCELLATION_REASON_ID))||
      isNullOrEmpty(holdCancellation.getString(FIELD_CANCELED_BY_USER_ID))
    );
  }

  private static boolean areRequiredHoldCancellationFieldsUUIDs(JsonObject holdCancellation) {
    return (!isUUID (holdCancellation.getString(FIELD_HOLD_ID)) ||
      !isUUID (holdCancellation.getString(FIELD_CANCELLATION_REASON_ID)) ||
      !isUUID (holdCancellation.getString(FIELD_CANCELED_BY_USER_ID))
    );
  }

  private static boolean isNullOrEmpty(String stringToCheck) {
    return stringToCheck == null || stringToCheck.isEmpty();
  }

  private static boolean isUUID(String uuidString) {
    try {
      UUID.fromString(uuidString);
      return true;
    } catch (Exception ex) {
      return false;
    }
  }
}
