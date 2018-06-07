package org.folio.edge.patron;

import java.util.Currency;

public class Constants {

  public static final String DEFAULT_CURRENCY_CODE = Currency.getInstance("USD").getCurrencyCode();

  public static final String PARAM_INCLUDE_LOANS = "includeLoans";
  public static final String PARAM_INCLUDE_CHARGES = "includeCharges";
  public static final String PARAM_INCLUDE_HOLDS = "includeHolds";
  public static final String PARAM_PATRON_ID = "patronId";
  public static final String PARAM_ITEM_ID = "itemId";
  public static final String PARAM_INSTANCE_ID = "instanceId";
  public static final String PARAM_HOLD_ID = "holdId";

  public static final String MSG_ACCESS_DENIED = "Access Denied";
  public static final String MSG_INTERNAL_SERVER_ERROR = "Internal Server Error";
  public static final String MSG_REQUEST_TIMEOUT = "Request to FOLIO timed out";
  public static final String MSG_NOT_IMPLEMENTED = "Not Implemented";

  private Constants() {

  }

}
