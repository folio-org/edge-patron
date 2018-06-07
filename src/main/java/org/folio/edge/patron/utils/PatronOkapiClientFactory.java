package org.folio.edge.patron.utils;

import org.folio.edge.core.utils.OkapiClientFactory;

import io.vertx.core.Vertx;

public class PatronOkapiClientFactory extends OkapiClientFactory {

  public PatronOkapiClientFactory(Vertx vertx, String okapiURL, long reqTimeoutMs) {
    super(vertx, okapiURL, reqTimeoutMs);
  }

  public PatronOkapiClient getPatronOkapiClient(String tenant) {
    return new PatronOkapiClient(vertx, okapiURL, tenant, reqTimeoutMs);
  }
}
