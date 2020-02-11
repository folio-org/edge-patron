package org.folio.edge.patron.model;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HoldCancellationValidatorTest {

  @Test
  public void validateCancelHoldRequestNullObject() {
    String result = HoldCancellationValidator.validateCancelHoldRequest(null);
    assertEquals("invalid holdCancellationRequest. null", result);
  }

  @Test
  public void validateRequiredHoldMissingCancellationFields() {
    String expectedErrorMsg = "required fields for cancelling holds are missing (holdId, cancellationReasonId)";
    String cancellationJsonMissingHoldId = "{" +
      "\"cancellationReasonId\" : \"" + UUID.randomUUID().toString() + "\"," +
      "\"cancellationAdditionalInformation\" : \"blablabla\"" +
      "}";
    String result = HoldCancellationValidator.validateCancelHoldRequest(new JsonObject(cancellationJsonMissingHoldId));
    assertEquals(expectedErrorMsg, result);

    String cancellationJsonMissingcancellationReasonId= "{" +
      "\"holdId\" :  \"" + UUID.randomUUID().toString() + "\"," +
      "\"cancellationAdditionalInformation\" : \"blablabla\"" +
      "}";
    result = HoldCancellationValidator.validateCancelHoldRequest(new JsonObject(cancellationJsonMissingcancellationReasonId));
    assertEquals(expectedErrorMsg, result);
  }

  @Test
  public void validateCancelHoldInvalidUUIDs() {
    String validUUID = "3a40852d-49fd-4df2-a1f9-6e2641a6e91f";
    String invalidUUID = "3a40852d-g9fd-fdf2-a1f9-6e2641a6e91p";
    String expectedErrorMsg = "invalid values for one of the required fields (holdId, cancellationReasonId, canceledByUserId)";

    String invalidHoldId = "{" +
    "\"holdId\" : \"" + invalidUUID + "\"," +
      "\"cancellationReasonId\" : \"" + validUUID + "\"," +
    "\"cancellationAdditionalInformation\" : \"blablabla\"" +
    "}";
    String result = HoldCancellationValidator.validateCancelHoldRequest(new JsonObject(invalidHoldId));
    assertEquals(expectedErrorMsg, result);

    String invalidCancellationReasonId = "{" +
      "\"holdId\" : \"" + validUUID + "\"," +
      "\"cancellationReasonId\" : \"" + invalidUUID + "\"," +
      "\"cancellationAdditionalInformation\" : \"blablabla\"" +
      "}";
    result = HoldCancellationValidator.validateCancelHoldRequest(new JsonObject(invalidCancellationReasonId));
    assertEquals(expectedErrorMsg, result);
  }

  @Test
  public void validateCancelHoldRequestValidParams() {
    String cancellationJson = "{" +
      "\"holdId\" : \"" + UUID.randomUUID().toString() + "\"," +
      "\"cancellationReasonId\" : \"" + UUID.randomUUID().toString() + "\"," +
      "\"cancellationAdditionalInformation\" : \"blablabla\"," +
      "\"canceledDate\" : \"2019-12-06T16:05:16.2165Z\"" +
      "}";
    String result = HoldCancellationValidator.validateCancelHoldRequest(new JsonObject(cancellationJson));
    assertNull(result);
  }
}
