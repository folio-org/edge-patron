package org.folio.edge.patron;

import static org.folio.edge.patron.Constants.DEFAULT_KEYCLOAK_KEY_CACHE_CAPACITY;
import static org.folio.edge.patron.Constants.DEFAULT_KEYCLOAK_KEY_CACHE_TTL_MS;
import static org.folio.edge.patron.Constants.DEFAULT_NULL_KEYCLOAK_KEY_CACHE_TTL_MS;
import static org.folio.edge.patron.Constants.DEFAULT_NULL_PATRON_ID_CACHE_TTL_MS;
import static org.folio.edge.patron.Constants.DEFAULT_PATRON_ID_CACHE_CAPACITY;
import static org.folio.edge.patron.Constants.DEFAULT_PATRON_ID_CACHE_TTL_MS;
import static org.folio.edge.patron.Constants.KEYCLOAK_URL;
import static org.folio.edge.patron.Constants.SYS_KEYCLOAK_KEY_CACHE_CAPACITY;
import static org.folio.edge.patron.Constants.SYS_KEYCLOAK_KEY_CACHE_TTL_MS;
import static org.folio.edge.patron.Constants.SYS_NULL_KEYCLOAK_KEY_CACHE_TTL_MS;
import static org.folio.edge.patron.Constants.SYS_NULL_PATRON_ID_CACHE_TTL_MS;
import static org.folio.edge.patron.Constants.SYS_PATRON_ID_CACHE_CAPACITY;
import static org.folio.edge.patron.Constants.SYS_PATRON_ID_CACHE_TTL_MS;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.EdgeVerticleHttp;
import org.folio.edge.core.utils.OkapiClientFactory;
import org.folio.edge.core.utils.OkapiClientFactoryInitializer;
import org.folio.edge.patron.cache.KeycloakPublicKeyCache;
import org.folio.edge.patron.cache.PatronIdCache;
import org.folio.edge.patron.utils.KeycloakClient;

public class MainVerticle extends EdgeVerticleHttp {

  private static final Logger logger = LogManager.getLogger(MainVerticle.class);

  public MainVerticle() {
    super();
    initializePatronIdCache();
    initializeKeycloakKeyCache();
  }

  private void initializePatronIdCache() {
    final String patronIdCacheTtlMs = System.getProperty(SYS_PATRON_ID_CACHE_TTL_MS);
    final long cacheTtlMs = patronIdCacheTtlMs != null ? Long.parseLong(patronIdCacheTtlMs)
      : DEFAULT_PATRON_ID_CACHE_TTL_MS;

    final String nullTokenCacheTtlMs = System.getProperty(SYS_NULL_PATRON_ID_CACHE_TTL_MS);
    final long failureCacheTtlMs = nullTokenCacheTtlMs != null ? Long.parseLong(nullTokenCacheTtlMs)
      : DEFAULT_NULL_PATRON_ID_CACHE_TTL_MS;

    final String patronIdCacheCapacity = System.getProperty(SYS_PATRON_ID_CACHE_CAPACITY);
    final int cacheCapacity = patronIdCacheCapacity != null ? Integer.parseInt(patronIdCacheCapacity)
      : DEFAULT_PATRON_ID_CACHE_CAPACITY;

    PatronIdCache.initialize(cacheTtlMs, failureCacheTtlMs, cacheCapacity);
  }

  private void initializeKeycloakKeyCache() {
    final String keycloakKeyCacheTtlMs = retriveProperty(SYS_KEYCLOAK_KEY_CACHE_TTL_MS);
    final long cacheTtlMs = keycloakKeyCacheTtlMs != null ? Long.parseLong(keycloakKeyCacheTtlMs)
      : DEFAULT_KEYCLOAK_KEY_CACHE_TTL_MS;

    final String nullTokenCacheTtlMs = retriveProperty(SYS_NULL_KEYCLOAK_KEY_CACHE_TTL_MS);
    final long failureCacheTtlMs = nullTokenCacheTtlMs != null ? Long.parseLong(nullTokenCacheTtlMs)
      : DEFAULT_NULL_KEYCLOAK_KEY_CACHE_TTL_MS;

    final String keycloakKeyCacheCapacity = retriveProperty(SYS_KEYCLOAK_KEY_CACHE_CAPACITY);
    final int cacheCapacity = keycloakKeyCacheCapacity != null ? Integer.parseInt(keycloakKeyCacheCapacity)
      : DEFAULT_KEYCLOAK_KEY_CACHE_CAPACITY;

    KeycloakPublicKeyCache.initialize(cacheTtlMs, failureCacheTtlMs, cacheCapacity);
  }

  @Override
  public Router defineRoutes() {
    OkapiClientFactory ocf = OkapiClientFactoryInitializer.createInstance(vertx, config());
    final String keycloakUrl = retriveProperty(KEYCLOAK_URL);
    if (keycloakUrl == null || keycloakUrl.isEmpty()) {
      logger.warn("Keycloak url is not defined. Secure endpoints will not work");
    }
    logger.info("Using keycloak url: {}", keycloakUrl);
    KeycloakClient keycloakClient = new KeycloakClient(keycloakUrl, WebClient.create(vertx));
    PatronHandler patronHandler = new PatronHandler(secureStore, ocf, keycloakClient);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router.route(HttpMethod.GET, "/admin/health")
      .handler(this::handleHealthCheck);

    router.route(HttpMethod.GET, "/patron/account/:patronId")
      .handler(patronHandler::handleGetAccount);

    router.route(HttpMethod.GET, "/patron/account")
      .handler(patronHandler::handleSecureGetAccount);

    router.route(HttpMethod.POST, "/patron/account/:patronId/item/:itemId/renew")
      .handler(patronHandler::handleRenew);

    router.route(HttpMethod.POST, "/patron/account/:patronId/item/:itemId/hold")
      .handler(patronHandler::handlePlaceItemHold);

    router.route(HttpMethod.POST, "/patron/account/item/:itemId/hold")
      .handler(patronHandler::handleSecurePlaceItemHold);

    router.route(HttpMethod.POST, "/patron")
      .handler(patronHandler::handlePostPatronRequest);

    router.route(HttpMethod.PUT, "/patron/:externalSystemId")
      .handler(patronHandler::handlePutPatronRequest);

    router.route(HttpMethod.POST, "/patron/account/:patronId/instance/:instanceId/hold")
      .handler(patronHandler::handlePlaceInstanceHold);

    router.route(HttpMethod.POST, "/patron/account/instance/:instanceId/hold")
      .handler(patronHandler::handleSecurePlaceInstanceHold);

    router.route(HttpMethod.GET, "/patron/account/:patronId/instance/:instanceId/allowed-service-points")
      .handler(patronHandler::handleGetAllowedServicePointsForInstance);

    router.route(HttpMethod.GET, "/patron/account/instance/:instanceId/allowed-service-points")
      .handler(patronHandler::handleSecureGetAllowedServicePointsForInstance);

    router.route(HttpMethod.GET, "/patron/account/:patronId/item/:itemId/allowed-service-points")
      .handler(patronHandler::handleGetAllowedServicePointsForItem);

    router.route(HttpMethod.GET, "/patron/account/item/:itemId/allowed-service-points")
      .handler(patronHandler::handleSecureGetAllowedServicePointsForItem);

    router.route(HttpMethod.POST, "/patron/account/:patronId/hold/:holdId/cancel")
      .handler(patronHandler::handleCancelHold);

    router.route(HttpMethod.POST, "/patron/account/hold/:holdId/cancel")
      .handler(patronHandler::handleSecureCancelHold);

    router.route(HttpMethod.GET, "/patron/registration-status")
      .handler(patronHandler::handleGetPatronRegistrationStatus);

    return router;
  }

  private String retriveProperty(String name) {
    var property = System.getProperty(name);
    if (property == null) {
      property = System.getenv().get(name);
    }
    return property;
  }
}
