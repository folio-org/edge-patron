package org.folio.edge.patron;

import static org.folio.edge.patron.Constants.DEFAULT_NULL_PATRON_ID_CACHE_TTL_MS;
import static org.folio.edge.patron.Constants.DEFAULT_PATRON_ID_CACHE_CAPACITY;
import static org.folio.edge.patron.Constants.DEFAULT_PATRON_ID_CACHE_TTL_MS;
import static org.folio.edge.patron.Constants.SYS_NULL_PATRON_ID_CACHE_TTL_MS;
import static org.folio.edge.patron.Constants.SYS_PATRON_ID_CACHE_CAPACITY;
import static org.folio.edge.patron.Constants.SYS_PATRON_ID_CACHE_TTL_MS;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.EdgeVerticleHttp;
import org.folio.edge.patron.cache.PatronIdCache;
import org.folio.edge.patron.utils.PatronOkapiClientFactory;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends EdgeVerticleHttp {

  private static final Logger logger = LogManager.getLogger(MainVerticle.class);

  public MainVerticle() {
    super();

    final String patronIdCacheTtlMs = System.getProperty(SYS_PATRON_ID_CACHE_TTL_MS);
    final long cacheTtlMs = patronIdCacheTtlMs != null ? Long.parseLong(patronIdCacheTtlMs)
        : DEFAULT_PATRON_ID_CACHE_TTL_MS;
    logger.info("Using patronId cache TTL (ms): " + patronIdCacheTtlMs);

    final String nullTokenCacheTtlMs = System.getProperty(SYS_NULL_PATRON_ID_CACHE_TTL_MS);
    final long failureCacheTtlMs = nullTokenCacheTtlMs != null ? Long.parseLong(nullTokenCacheTtlMs)
        : DEFAULT_NULL_PATRON_ID_CACHE_TTL_MS;
    logger.info("Using patronId cache TTL (ms): " + failureCacheTtlMs);

    final String patronIdCacheCapacity = System.getProperty(SYS_PATRON_ID_CACHE_CAPACITY);
    final int cacheCapacity = patronIdCacheCapacity != null ? Integer.parseInt(patronIdCacheCapacity)
        : DEFAULT_PATRON_ID_CACHE_CAPACITY;
    logger.info("Using patronId cache capacity: " + patronIdCacheCapacity);

    // initialize the TokenCache
    PatronIdCache.initialize(cacheTtlMs, failureCacheTtlMs, cacheCapacity);
  }

  @Override
  public Router defineRoutes() {
    PatronOkapiClientFactory ocf = new PatronOkapiClientFactory(vertx, config().getString(org.folio.edge.core.Constants.SYS_OKAPI_URL),
      config().getInteger(org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS));
    PatronHandler patronHandler = new PatronHandler(secureStore, ocf);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router.route(HttpMethod.GET, "/admin/health")
      .handler(this::handleHealthCheck);

    router.route(HttpMethod.GET, "/patron/account/:patronId")
      .handler(patronHandler::handleGetAccount);

    router.route(HttpMethod.POST, "/patron/account/:patronId/item/:itemId/renew")
      .handler(patronHandler::handleRenew);

    router.route(HttpMethod.POST, "/patron/account/:patronId/item/:itemId/hold")
      .handler(patronHandler::handlePlaceItemHold);

    router.route(HttpMethod.POST, "/patron/account/:patronId/instance/:instanceId/hold")
      .handler(patronHandler::handlePlaceInstanceHold);

    router.route(HttpMethod.POST, "/patron/account/:patronId/hold/:holdId/cancel")
      .handler(patronHandler::handleCancelHold);

    return router;
  }
}
