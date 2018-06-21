package org.folio.edge.patron.utils;

import java.util.concurrent.CompletableFuture;

import org.apache.log4j.Logger;
import org.folio.edge.core.cache.TokenCache.NotInitializedException;
import org.folio.edge.patron.cache.PatronIdCache;

public class PatronIdHelper {

  private static final Logger logger = Logger.getLogger(PatronIdHelper.class);

  private PatronIdHelper() {

  }

  public static CompletableFuture<String> lookupPatron(PatronOkapiClient client, String tenant, String extPatronId) {
    CompletableFuture<String> future = new CompletableFuture<>();

    String patronId = null;
    try {
      PatronIdCache cache = PatronIdCache.getInstance();
      patronId = cache.get(tenant, extPatronId);
    } catch (NotInitializedException e) {
      logger.warn("Failed to access PatronIdCache", e);
    }

    if (patronId != null) {
      logger.info("Using cached patronId");
      future.complete(patronId);
    } else {
      client.getPatron(extPatronId).exceptionally(t -> {
        logger.error("Patron lookup failed for " + extPatronId, t);
        future.completeExceptionally(t);
        return null;
      }).thenAcceptAsync(internalId -> {
        logger.info(String.format("Patron lookup successful: %s -> %s", extPatronId, internalId));
        future.complete(internalId);
      });
    }
    return future;
  }

}
