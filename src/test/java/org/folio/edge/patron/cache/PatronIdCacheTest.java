package org.folio.edge.patron.cache;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.cache.Cache.CacheValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PatronIdCacheTest {

  private static final Logger logger = LogManager.getLogger(PatronIdCacheTest.class);

  private static final int cap = 50;
  private static final long ttl = 3000;
  private static final long nullValueTtl = 1000;

  private static final String tenant = "diku";
  private static final String extPatronId = UUID.randomUUID().toString();
  private static final String patronId = UUID.randomUUID().toString();

  @BeforeEach
  void setUp() {
    // initialize singleton cache
    PatronIdCache.initialize(ttl, nullValueTtl, cap);
  }

  @Test
  void testReinitialize() {
    logger.info("=== Test Reinitialize... ===");
    final CacheValue<String> cached = PatronIdCache.getInstance().put(tenant, extPatronId, patronId);

    await().with()
      .pollInterval(20, TimeUnit.MILLISECONDS)
      .atMost(ttl + 100, TimeUnit.MILLISECONDS)
      .until(cached::expired);

    PatronIdCache.initialize(ttl * 2, nullValueTtl, cap);
    final CacheValue<String> cached2 = PatronIdCache.getInstance().put(tenant, extPatronId, patronId);

    await().with()
      .pollInterval(20, TimeUnit.MILLISECONDS)
      .atLeast(ttl, TimeUnit.MILLISECONDS)
      .atMost(ttl * 2 + 100, TimeUnit.MILLISECONDS)
      .until(cached2::expired);
  }

  @Test
  void testEmpty() {
    logger.info("=== Test that a new cache is empty... ===");

    // empty cache...
    assertNull(PatronIdCache.getInstance().get(tenant, extPatronId));
  }

  @Test
  void testGetPutGet() {
    logger.info("=== Test basic functionality (Get, Put, Get)... ===");

    PatronIdCache cache = PatronIdCache.getInstance();

    // empty cache...
    assertNull(cache.get(tenant, extPatronId));

    // basic functionality
    cache.put(tenant, extPatronId, patronId);
    assertEquals(patronId, cache.get(tenant, extPatronId));
  }

  @Test
  void testNoOverwrite() {
    logger.info("=== Test entries aren't overwritten... ===");

    PatronIdCache cache = PatronIdCache.getInstance();
    String baseVal = "patronId";

    // make sure we don't overwrite the cached patronIdue
    cache.put(tenant, extPatronId, baseVal);
    assertEquals(baseVal, cache.get(tenant, extPatronId));

    for (int i = 0; i < 100; i++) {
      cache.put(tenant, extPatronId, baseVal + i);
      assertEquals(baseVal, cache.get(tenant, extPatronId));
    }

    // should expire very soon, if not already.
    await().with()
      .pollInterval(20, TimeUnit.MILLISECONDS)
      .atMost(ttl + 100, TimeUnit.MILLISECONDS)
      .until(() -> cache.get(tenant, extPatronId) == null);
  }

  @Test
  void testPruneExpires() {
    logger.info("=== Test pruning of expired entries... ===");

    PatronIdCache cache = PatronIdCache.getInstance();
    String baseVal = "patronId";

    CacheValue<String> cached = cache.put(tenant, extPatronId + 0, baseVal);
    await().with()
      .pollInterval(20, TimeUnit.MILLISECONDS)
      .atMost(ttl + 100, TimeUnit.MILLISECONDS)
      .until(cached::expired);

    // load capacity + 1 entries triggering eviction of expired
    for (int i = 1; i <= cap; i++) {
      cache.put(tenant, extPatronId + i, baseVal + i);
    }

    // should be evicted as it's expired
    assertNull(cache.get(tenant, extPatronId + 0));

    // should still be cached
    for (int i = 1; i <= cap; i++) {
      assertEquals(baseVal + i, cache.get(tenant, extPatronId + i));
    }
  }

  @Test
  void testPruneNoExpires() {
    logger.info("=== Test pruning of unexpired entries... ===");

    PatronIdCache cache = PatronIdCache.getInstance();
    String baseVal = "patronId";

    // load capacity + 1 entries triggering eviction of the first
    for (int i = 0; i <= cap; i++) {
      cache.put(tenant, extPatronId + i, baseVal + i);
    }

    // should be evicted as it's the oldest
    assertNull(cache.get(tenant, extPatronId + 0));

    // should still be cached
    for (int i = 1; i <= cap; i++) {
      assertEquals(baseVal + i, cache.get(tenant, extPatronId + i));
    }
  }

  @Test
  void testNullPatronIdExpires() {
    logger.info("=== Test expiration of null patronId entries... ===");

    PatronIdCache cache = PatronIdCache.getInstance();

    CacheValue<String> cached = cache.put(tenant, extPatronId, null);

    assertNull(cache.get(tenant, extPatronId));

    await().with()
      .pollInterval(20, TimeUnit.MILLISECONDS)
      .atMost(nullValueTtl + 100, TimeUnit.MILLISECONDS)
      .until(cached::expired);

    assertNull(cache.get(tenant, extPatronId));
  }
}
