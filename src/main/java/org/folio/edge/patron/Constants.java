package org.folio.edge.patron;

import java.util.Currency;

public class Constants {

  public static final String KEYCLOAK_URL = "KC_URL";
  public static final String SYS_PATRON_ID_CACHE_TTL_MS = "patron_id_cache_ttl_ms";
  public static final String SYS_NULL_PATRON_ID_CACHE_TTL_MS = "null_patron_id_cache_ttl_ms";
  public static final String SYS_PATRON_ID_CACHE_CAPACITY = "patron_id_cache_capacity";
  public static final String SYS_KEYCLOAK_KEY_CACHE_TTL_MS = "keycloak_key_cache_ttl_ms";
  public static final String SYS_NULL_KEYCLOAK_KEY_CACHE_TTL_MS = "null_keycloak_key_cache_ttl_ms";
  public static final String SYS_KEYCLOAK_KEY_CACHE_CAPACITY = "keycloak_key_cache_capacity";
  public static final String PARAM_EXPIRED = "expired";
  public static final String VIP_CLAIM = "vip";
  public static final String EXTERNAL_SYSTEM_ID_CLAIM = "externalSystemId";

  public static final String DEFAULT_CURRENCY_CODE = Currency.getInstance("USD").getCurrencyCode();
  public static final long DEFAULT_PATRON_ID_CACHE_TTL_MS = 60 * 60 * 1000L;
  public static final long DEFAULT_NULL_PATRON_ID_CACHE_TTL_MS = 30 * 1000L;
  public static final int DEFAULT_PATRON_ID_CACHE_CAPACITY = 1000;
  public static final long DEFAULT_KEYCLOAK_KEY_CACHE_TTL_MS = 60 * 60 * 1000L;
  public static final long DEFAULT_NULL_KEYCLOAK_KEY_CACHE_TTL_MS = 30 * 1000L;
  public static final int DEFAULT_KEYCLOAK_KEY_CACHE_CAPACITY = 50;

  public static final String PARAM_SORT_BY = "sortBy";
  public static final String PARAM_LIMIT = "limit";
  public static final String PARAM_OFFSET = "offset";
  public static final String PARAM_INCLUDE_LOANS = "includeLoans";
  public static final String PARAM_INCLUDE_CHARGES = "includeCharges";
  public static final String PARAM_INCLUDE_HOLDS = "includeHolds";
  public static final String PARAM_PATRON_ID = "patronId";
  public static final String PARAM_ITEM_ID = "itemId";
  public static final String PARAM_INSTANCE_ID = "instanceId";
  public static final String PARAM_HOLD_ID = "holdId";
  public static final String PARAM_EMAIL_ID = "emailId";
  public static final String PARAM_EXTERNAL_SYSTEM_ID = "externalSystemId";
  public static final String PARAM_REQUEST_ID = "requestId";

  public static final String MSG_ACCESS_DENIED = "Access Denied";
  public static final String MSG_INTERNAL_SERVER_ERROR = "Internal Server Error";
  public static final String MSG_REQUEST_TIMEOUT = "Request to FOLIO timed out";
  public static final String MSG_HOLD_NOBODY = "No hold data provided";
  public static final String MSG_EXTERNAL_NOBODY = "No external_patron data provided";

  public static final String FIELD_EXPIRATION_DATE = "expirationDate";
  public static final String FIELD_REQUEST_DATE = "requestDate";

  public static final String FIELD_HOLD_ID = "holdId";
  public static final String FIELD_CANCELLATION_REASON_ID = "cancellationReasonId";
  public static final String FIELD_CANCELLATION_ADDITIONAL_INFO = "cancellationAdditionalInformation";
  public static final String FIELD_CANCELED_DATE = "canceledDate";

  private Constants() {
  }
}
