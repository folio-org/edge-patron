package org.folio.edge.patron.utils;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.test.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class PatronOkapiClientCompressionTest {
  private static final Logger logger = LogManager.getLogger(PatronOkapiClientCompressionTest.class);

  private final Vertx vertx = Vertx.vertx();

  private final String patronId = UUID.randomUUID().toString();
  private static final String tenant = "diku";
  private static final int reqTimeout = 3000;

  private PatronOkapiClient client;

  @Before
  public void setUp(TestContext context) throws Exception {
    int okapiPort = TestUtils.getPort();

    final HttpServer server = vertx.createHttpServer(
        new HttpServerOptions().setCompressionSupported(true));

    server.requestHandler(req -> {
      req.response()
        .setStatusCode(200)
        .putHeader("content-type", "application/json")
        .end("{\"test\":\"1234\"}");
    });

    final Async async = context.async();
    server.listen(okapiPort, "localhost", ar -> {
      context.assertTrue(ar.succeeded());
      async.complete();
    });

    client = new PatronOkapiClientFactory(vertx,
        "http://localhost:" + okapiPort, reqTimeout).getPatronOkapiClient(tenant);
  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testCompression(TestContext context) throws Exception {
    logger.info("=== Test Compression ===");

    MultiMap headers = MultiMap.caseInsensitiveMultiMap();
    headers.add("Accept-Encoding", "gzip");
    Async async = context.async();
    client.getAccount(patronId,
        true,
        true,
        true,
        null,
        null,
        null,
        headers,
        resp -> {
          logger.info("mod-patron response body: " + resp.body());
          context.assertEquals("{\"test\":\"1234\"}", resp.bodyAsString());
          async.complete();
        },
        context::fail);
  }
}
