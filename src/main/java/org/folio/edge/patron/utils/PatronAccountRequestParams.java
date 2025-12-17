package org.folio.edge.patron.utils;

public record PatronAccountRequestParams(String patronId, boolean includeLoans, boolean includeCharges, boolean includeHolds, boolean includeBatches,
                                         String sortBy, String limit, String offset) {

  static PatronAccountRequestParams defaultParams(String patronId) {
    return new PatronAccountRequestParams(patronId, false, false, false, false, null, null, null);
  }
}
