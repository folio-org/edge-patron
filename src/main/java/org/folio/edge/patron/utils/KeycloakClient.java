package org.folio.edge.patron.utils;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.web.client.WebClient;
import org.apache.http.HttpStatus;
import org.folio.edge.patron.cache.KeycloakPublicKeyCache;

public class KeycloakClient {

  private static final String REALM_INFO_URI = "/realms/%s/protocol/openid-connect/certs";
  private final String keycloakUrl;
  private final WebClient webClient;

  public KeycloakClient(String keycloakUrl, WebClient webClient) {
    this.keycloakUrl = keycloakUrl;
    this.webClient = webClient;
  }

  public Future<String> getPublicKeys(String realm) {
    Promise<String> promise = Promise.promise();
    String uri = String.format(REALM_INFO_URI, realm);
    webClient.getAbs(keycloakUrl + uri).send()
      .onSuccess(response -> {
        if (HttpStatus.SC_OK == response.statusCode()) {
          var body = response.bodyAsString();
          KeycloakPublicKeyCache.getInstance().put(realm, body);
          promise.complete(body);
        } else {
          promise.fail(new RuntimeException("Request failed with status: " + response.statusCode()));
        }
      })
      .onFailure(promise::fail);
    return promise.future();
  }
}
