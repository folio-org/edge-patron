package org.folio.edge.patron.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.ProtectedHeader;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import java.security.Key;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.patron.cache.KeycloakPublicKeyCache;

public class KeycloakTokenHelper {

  private static final Logger logger = LogManager.getLogger(KeycloakTokenHelper.class);

  private KeycloakTokenHelper() {
  }

  public static Future<Claims> getClaimsFromToken(String accessToken, String realm, KeycloakClient client) {
    Promise<Claims> promise = Promise.promise();
    getKeycloakPublicKey(realm, client).onSuccess(keys -> {
      var jwks = Jwks.setParser().build().parse(keys);
      var parser = Jwts.parser().keyLocator(locateKey(jwks)).build();
      try {
        var claims = parser.parseSignedClaims(accessToken).getPayload();
        promise.complete(claims);
      } catch (Exception ex) {
        promise.fail(ex);
      }
    }).onFailure(ex -> {
      logger.error("Failed to get public key from keycloak", ex);
      promise.fail(ex);
    });
    return promise.future();
  }

  private static Locator<Key> locateKey(JwkSet jwks) {
    return header -> {
      if (header instanceof ProtectedHeader ph) {
        var key = jwks.getKeys().stream().filter(jwk -> jwk.getId().equals(ph.getKeyId())).findFirst();
        if (key.isEmpty()) {
          return null;
        }
        return key.get().toKey();
      } else {
        return null;
      }
    };
  }

  private static Future<String> getKeycloakPublicKey(String realm, KeycloakClient client) {
    String publicKey = null;
    try {
      var cache = KeycloakPublicKeyCache.getInstance();
      publicKey = cache.get(realm);
    } catch (KeycloakPublicKeyCache.KeycloakPublicKeyCacheNotInitializedException ex) {
      logger.warn("Keycloak cache not initialized");
    }
    if (publicKey != null) {
      return Future.succeededFuture(publicKey);
    }

    return client.getPublicKeys(realm);
  }

}
