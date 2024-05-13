package org.folio.edge.patron.utils;

import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import io.vertx.core.json.JsonObject;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.edge.core.utils.OkapiClientFactory;
import org.junit.Test;

import io.vertx.core.Vertx;

public class PatronOkapiClientFactoryTest {

  @Test
  public void testGetOkapiClient() {
    Vertx vertx = Vertx.vertx();
    int reqTimeout = 5000;
    JsonObject config = new JsonObject()
      .put(SYS_OKAPI_URL, "http://mocked.okapi:9130")
      .put(SYS_REQUEST_TIMEOUT_MS, reqTimeout);
    OkapiClientFactory ocf = PatronOkapiClientFactory.createInstance(vertx, config);
    OkapiClient client = ocf.getOkapiClient("tenant");
    assertNotNull(client);
    assertEquals(reqTimeout, client.reqTimeout);
  }
}
