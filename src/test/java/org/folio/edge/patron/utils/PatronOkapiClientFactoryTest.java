package org.folio.edge.patron.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.Vertx;

public class PatronOkapiClientFactoryTest {

  private static final int reqTimeout = 5000;

  private PatronOkapiClientFactory ocf;

  @Before
  public void setUp() throws Exception {

    Vertx vertx = Vertx.vertx();
    ocf = new PatronOkapiClientFactory(vertx, "http://mocked.okapi:9130", reqTimeout);
  }

  @Test
  public void testGetOkapiClient() {
    PatronOkapiClient client = ocf.getPatronOkapiClient("tenant");
    assertNotNull(client);
    assertEquals(reqTimeout, client.reqTimeout);
  }
}
