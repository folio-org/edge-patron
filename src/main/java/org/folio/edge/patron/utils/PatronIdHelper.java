package org.folio.edge.patron.utils;

import io.vertx.core.Future;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.patron.cache.PatronIdCache;

public class PatronIdHelper {

  private static final Logger logger = LogManager.getLogger(PatronIdHelper.class);

  private PatronIdHelper() {

  }

  public static Future<String> lookupPatron(PatronOkapiClient client, String tenant, String extPatronId) {
    logger.info("looking up patronId={}...", extPatronId);
    String patronId = null;
    try {
      PatronIdCache cache = PatronIdCache.getInstance();
      patronId = cache.get(tenant, extPatronId);
    } catch (PatronIdCache.PatronIdCacheNotInitializedException e) {
      logger.warn("Failed to access PatronIdCache", e);
    }

    if (patronId != null) {
      logger.info("Using cached patronId");
      return Future.succeededFuture(patronId);
    }

    return client.getPatron(extPatronId)
        .onSuccess(internalId -> logger.info("Patron lookup successful: {} -> {}", extPatronId, internalId))
        .onFailure(t -> logger.error("Patron lookup failed for {}", extPatronId, t));
  }

}
