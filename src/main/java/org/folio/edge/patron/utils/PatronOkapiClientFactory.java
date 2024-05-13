package org.folio.edge.patron.utils;

import static org.folio.edge.core.Constants.SYS_KEYSTORE_PASSWORD;
import static org.folio.edge.core.Constants.SYS_KEYSTORE_PATH;
import static org.folio.edge.core.Constants.SYS_KEY_ALIAS;
import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.OkapiClientFactory;

import io.vertx.core.Vertx;

public class PatronOkapiClientFactory {
  private static final Logger logger = LogManager.getLogger(PatronOkapiClientFactory.class);

  private PatronOkapiClientFactory() {
  }

  public static OkapiClientFactory createInstance(Vertx vertx, JsonObject config) {
    String okapiUrl = config.getString(SYS_OKAPI_URL);
    Integer requestTimeout = config.getInteger(SYS_REQUEST_TIMEOUT_MS);
    String keystorePath = config.getString(SYS_KEYSTORE_PATH);
    String keystorePassword = config.getString(SYS_KEYSTORE_PASSWORD);
    String keyAlias = config.getString(SYS_KEY_ALIAS);
    if (keystorePath != null && keystorePassword != null) {
      logger.info("Creating OkapiClientFactory with Enhance HTTP Endpoint Security and TLS mode enabled");
      return new OkapiClientFactory(vertx, okapiUrl, requestTimeout, keystorePath, keystorePassword, keyAlias);
    } else {
      return new OkapiClientFactory(vertx, okapiUrl,  requestTimeout);
    }
  }
}
