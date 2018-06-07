package org.folio.edge.patron.utils;

import static org.folio.edge.core.utils.test.MockOkapi.MOCK_TOKEN;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.folio.edge.core.utils.test.TestUtils;
import org.folio.edge.patron.model.Hold;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class PatronOkapiClientTest {

  private static final Logger logger = Logger.getLogger(PatronOkapiClientTest.class);

  private final String patronId = UUID.randomUUID().toString();
  private final String itemId = UUID.randomUUID().toString();
  private static final String tenant = "diku";
  private static final long reqTimeout = 3000L;

  private PatronOkapiClient client;
  private PatronMockOkapi mockOkapi;

  @Before
  public void setUp(TestContext context) throws Exception {
    int okapiPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(tenant);

    mockOkapi = new PatronMockOkapi(okapiPort, knownTenants);
    mockOkapi.start(context);

    client = new PatronOkapiClientFactory(Vertx.vertx(), "http://localhost:" + okapiPort, reqTimeout)
      .getPatronOkapiClient(tenant);
  }

  @After
  public void tearDown(TestContext context) {
    mockOkapi.close();
  }

  @Test
  public void testGetAccountWithAll(TestContext context) throws Exception {
    logger.info("=== Test successful getAccount request w/ all data ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      client.getAccount(patronId,
          true,
          true,
          true,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(PatronMockOkapi.getAccountJson(patronId, true, true, true), body.toString());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testGetAccountNotFound(TestContext context) throws Exception {
    logger.info("=== Test getAccount - patron not found ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      client.getAccount(PatronMockOkapi.patronId_notFound,
          true,
          true,
          true,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(404, resp.statusCode());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testGetAccountNoCharges(TestContext context) throws Exception {
    logger.info("=== Test successful getAccount request w/o charges data ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      client.getAccount(patronId,
          true,
          true,
          false,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(PatronMockOkapi.getAccountJson(patronId, true, true, false), body.toString());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testGetAccountNoHolds(TestContext context) throws Exception {
    logger.info("=== Test successful getAccount request w/o holds data ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      client.getAccount(patronId,
          true,
          false,
          true,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(PatronMockOkapi.getAccountJson(patronId, true, false, true), body.toString());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testGetAccountNoLoans(TestContext context) throws Exception {
    logger.info("=== Test successful getAccount request w/o loans data ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      client.getAccount(patronId,
          false,
          true,
          true,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(PatronMockOkapi.getAccountJson(patronId, false, true, true), body.toString());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testGetAccountBaseOnly(TestContext context) throws Exception {
    logger.info("=== Test successful base getAccount request ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      client.getAccount(patronId,
          false,
          false,
          false,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(PatronMockOkapi.getAccountJson(patronId, false, false, false), body.toString());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testGetAccountNoToken(TestContext context) throws Exception {
    logger.info("=== Test getAccount w/o a token ===");

    Async async = context.async();
    client.getAccount(patronId,
        false,
        false,
        false,
        resp -> resp.bodyHandler(body -> {
          logger.info("mod-patron response body: " + body);
          context.assertEquals(403, resp.statusCode());
          async.complete();
        }),
        t -> {
          context.fail(t);
        });
  }

  @Test
  public void testRenewItemExistent(TestContext context) {
    logger.info("=== Test renewItem exists ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.renewItem(patronId,
          itemId,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(201, resp.statusCode());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testRenewItemNonExistentItem(TestContext context) {
    logger.info("=== Test renewItem item doesn't exist ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.renewItem(patronId,
          PatronMockOkapi.itemId_notFound,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(404, resp.statusCode());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testRenewItemNonExistentPatron(TestContext context) {
    logger.info("=== Test renewItem patron doesn't exist ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.renewItem(PatronMockOkapi.patronId_notFound,
          itemId,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(404, resp.statusCode());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testPlaceHoldExistent(TestContext context) throws Exception {
    logger.info("=== Test placeItemHold exists ===");

    Hold hold = PatronMockOkapi.getHold(itemId);
    String holdJSON = hold.toJson();

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.placeItemHold(patronId,
          itemId,
          holdJSON,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(201, resp.statusCode());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testPlaceHoldNonExistentItem(TestContext context) throws Exception {
    logger.info("=== Test placeItemHold item doesn't exist ===");

    Hold hold = PatronMockOkapi.getHold(PatronMockOkapi.itemId_notFound);
    String holdJSON = hold.toJson();

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.placeItemHold(patronId,
          PatronMockOkapi.itemId_notFound,
          holdJSON,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(404, resp.statusCode());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testPlaceHoldNonExistentPatron(TestContext context) throws Exception {
    logger.info("=== Test placeItemHold patron doesn't exist ===");

    Hold hold = PatronMockOkapi.getHold(itemId);
    String holdJSON = hold.toJson();

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.placeItemHold(PatronMockOkapi.patronId_notFound,
          itemId,
          holdJSON,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(404, resp.statusCode());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testRemoveItemHoldExistent(TestContext context) throws Exception {
    logger.info("=== Test removeItemHold exists ===");

    Hold hold = PatronMockOkapi.getHold(itemId);

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.removeItemHold(patronId,
          itemId,
          hold.requestId,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(201, resp.statusCode());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testRemoveItemHoldNonExistentItem(TestContext context) throws Exception {
    logger.info("=== Test removeItemHold item doesn't exist ===");

    Hold hold = PatronMockOkapi.getHold(PatronMockOkapi.itemId_notFound);

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.removeItemHold(patronId,
          PatronMockOkapi.itemId_notFound,
          hold.requestId,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(404, resp.statusCode());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testRemoveItemHoldNonExistentPatron(TestContext context) throws Exception {
    logger.info("=== Test removeItemHold patron doesn't exist ===");

    Hold hold = PatronMockOkapi.getHold(itemId);

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.removeItemHold(PatronMockOkapi.patronId_notFound,
          itemId,
          hold.requestId,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(404, resp.statusCode());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testRemoveItemHoldNonExistentHold(TestContext context) throws Exception {
    logger.info("=== Test removeItemHold hold doesn't exist ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.removeItemHold(patronId,
          itemId,
          PatronMockOkapi.holdReqId_notFound,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(404, resp.statusCode());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testEditItemHoldExistent(TestContext context) throws Exception {
    logger.info("=== Test editItemHold exists ===");

    Hold hold = PatronMockOkapi.getHold(itemId);

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.editItemHold(patronId,
          itemId,
          hold.requestId,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(501, resp.statusCode());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testEditItemHoldNonExistentItem(TestContext context) throws Exception {
    logger.info("=== Test editItemHold item doesn't exist ===");

    Hold hold = PatronMockOkapi.getHold(PatronMockOkapi.itemId_notFound);

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.editItemHold(patronId,
          PatronMockOkapi.itemId_notFound,
          hold.requestId,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(501, resp.statusCode());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testEditItemIHoldNonExistentPatron(TestContext context) throws Exception {
    logger.info("=== Test editItemHold patron doesn't exist ===");

    Hold hold = PatronMockOkapi.getHold(itemId);

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.editItemHold(PatronMockOkapi.patronId_notFound,
          itemId,
          hold.requestId,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(501, resp.statusCode());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testEditItemHoldNonExistentHold(TestContext context) throws Exception {
    logger.info("=== Test editItemHold hold doesn't exist ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.editItemHold(patronId,
          itemId,
          PatronMockOkapi.holdReqId_notFound,
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(501, resp.statusCode());
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }
}
