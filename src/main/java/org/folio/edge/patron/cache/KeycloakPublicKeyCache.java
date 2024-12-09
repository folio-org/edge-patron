package org.folio.edge.patron.cache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.cache.Cache;
import org.folio.edge.core.cache.Cache.Builder;
import org.folio.edge.core.cache.Cache.CacheValue;

public class KeycloakPublicKeyCache {

  private static final Logger logger = LogManager.getLogger(KeycloakPublicKeyCache.class);

  private static KeycloakPublicKeyCache instance = null;

  private Cache<String> cache;

  private KeycloakPublicKeyCache(long ttl, long nullTokenTtl, int capacity) {
    logger.info("Using TTL: {}", ttl);
    logger.info("Using null token TTL: {}", nullTokenTtl);
    logger.info("Using capacity: {}", capacity);
    cache = new Builder<String>()
      .withTTL(ttl)
      .withNullValueTTL(nullTokenTtl)
      .withCapacity(capacity)
      .build();
  }

  /**
   * Get the KeycloakPublicKeyCache singleton. the singleton must be initialized before
   * calling this method.
   *
   * @see {@link #initialize(long, long, int)}
   *
   * @return the KeycloakPublicKeyCache singleton instance.
   */
  public static synchronized KeycloakPublicKeyCache getInstance() {
    if (instance == null) {
      throw new KeycloakPublicKeyCacheNotInitializedException(
          "You must call KeycloakPublicKeyCache.initialize(ttl, capacity) before you can get the singleton instance");
    }
    return instance;
  }

  /**
   * Creates a new KeycloakPublicKeyCache instance, replacing the existing one if it
   * already exists; in which case all pre-existing cache entries will be lost.
   *
   * @param ttl
   *          cache entry time to live in ms
   * @param capacity
   *          maximum number of entries this cache will hold before pruning
   * @return the new KeycloakPublicKeyCache singleton instance
   */
  public static synchronized KeycloakPublicKeyCache initialize(long ttl, long nullValueTtl, int capacity) {
    if (instance != null) {
      logger.warn("Reinitializing cache.  All cached entries will be lost");
    }
    instance = new KeycloakPublicKeyCache(ttl, nullValueTtl, capacity);
    return instance;
  }

  public String get(String realm) {
    return cache.get(realm);
  }

  public CacheValue<String> put(String realm, String publicKey) {
    return cache.put(realm, publicKey);
  }

  public static class KeycloakPublicKeyCacheNotInitializedException extends RuntimeException {

    private static final long serialVersionUID = -8622978462142499585L;

    public KeycloakPublicKeyCacheNotInitializedException(String msg) {
      super(msg);
    }
  }

}
