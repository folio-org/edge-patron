package org.folio.edge.patron.cache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.cache.Cache;
import org.folio.edge.core.cache.Cache.Builder;
import org.folio.edge.core.cache.Cache.CacheValue;

public class PatronIdCache {

  private static final Logger logger = LogManager.getLogger(PatronIdCache.class);

  private static PatronIdCache instance = null;

  private Cache<String> cache;

  private PatronIdCache(long ttl, long nullTokenTtl, int capacity) {
    logger.info("Using TTL: {0}", ttl);
    logger.info("Using null token TTL: {0}", nullTokenTtl);
    logger.info("Using capcity: {0}", capacity);
    cache = new Builder<String>()
      .withTTL(ttl)
      .withNullValueTTL(nullTokenTtl)
      .withCapacity(capacity)
      .build();
  }

  /**
   * Get the PatronIdCache singleton. the singleton must be initialize before
   * calling this method.
   *
   * @see {@link #initialize(long, int)}
   *
   * @return the PatronIdCache singleton instance.
   */
  public static synchronized PatronIdCache getInstance() {
    if (instance == null) {
      throw new NotInitializedException(
          "You must call PatronIdCache.initialize(ttl, capacity) before you can get the singleton instance");
    }
    return instance;
  }

  /**
   * Creates a new PatronIdCache instance, replacing the existing one if it
   * already exists; in which case all pre-existing cache entries will be lost.
   *
   * @param ttl
   *          cache entry time to live in ms
   * @param capacity
   *          maximum number of entries this cache will hold before pruning
   * @return the new PatronIdCache singleton instance
   */
  public static synchronized PatronIdCache initialize(long ttl, long nullValueTtl, int capacity) {
    if (instance != null) {
      logger.warn("Reinitializing cache.  All cached entries will be lost");
    }
    instance = new PatronIdCache(ttl, nullValueTtl, capacity);
    return instance;
  }

  public String get(String tenant, String externalId) {
    return cache.get(computeKey(tenant, externalId));
  }

  public CacheValue<String> put(String tenant, String externalId, String internalId) {
    return cache.put(computeKey(tenant, externalId), internalId);
  }

  private String computeKey(String tenant, String externalId) {
    return String.format("%s:%s", tenant, externalId);
  }

  public static class NotInitializedException extends RuntimeException {

    private static final long serialVersionUID = 4747532964596334577L;

    public NotInitializedException(String msg) {
      super(msg);
    }
  }

}
