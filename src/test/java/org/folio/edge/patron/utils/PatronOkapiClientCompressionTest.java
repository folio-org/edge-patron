package org.folio.edge.patron.utils;

import java.util.UUID;

import org.apache.log4j.Logger;
import org.folio.edge.core.utils.test.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class PatronOkapiClientCompressionTest {
  private static final Logger logger = Logger.getLogger(PatronOkapiClientCompressionTest.class);

  private final Vertx vertx = Vertx.vertx();

  private final String patronId = UUID.randomUUID().toString();
  private static final String tenant = "diku";
  private static final long reqTimeout = 3000L;

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
        headers,
        resp -> resp.bodyHandler(body -> {
          logger.info("mod-patron response body: " + body);
          context.assertEquals("{\"test\":\"1234\"}", body.toString());
          async.complete();
        }),
        t -> {
          context.fail(t);
        });
  }
}
