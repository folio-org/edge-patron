package org.folio.edge.patron.utils;

import static org.folio.edge.core.Constants.SYS_KEYSTORE_PASSWORD;
import static org.folio.edge.core.Constants.SYS_KEYSTORE_PATH;
import static org.folio.edge.core.Constants.SYS_KEYSTORE_PROVIDER;
import static org.folio.edge.core.Constants.SYS_KEYSTORE_TYPE;
import static org.folio.edge.core.Constants.SYS_KEY_ALIAS;
import static org.folio.edge.core.Constants.SYS_KEY_ALIAS_PASSWORD;
import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.SYS_SSL_ENABLED;

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
    boolean isSslEnabled = config.getBoolean(SYS_SSL_ENABLED);
    if (isSslEnabled) {
      logger.info("Creating OkapiClientFactory with Enhance HTTP Endpoint Security and TLS mode enabled");
      String keystoreType = config.getString(SYS_KEYSTORE_TYPE);
      String keystoreProvider = config.getString(SYS_KEYSTORE_PROVIDER);
      String keystorePath = config.getString(SYS_KEYSTORE_PATH);
      String keystorePassword = config.getString(SYS_KEYSTORE_PASSWORD);
      String keyAlias = config.getString(SYS_KEY_ALIAS);
      String keyAliasPassword = config.getString(SYS_KEY_ALIAS_PASSWORD);
      return new OkapiClientFactory(vertx, okapiUrl, requestTimeout, keystoreType, keystoreProvider, keystorePath, keystorePassword, keyAlias, keyAliasPassword);
    } else {
      return new OkapiClientFactory(vertx, okapiUrl,  requestTimeout);
    }
  }
}
