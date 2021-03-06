package org.folio.edge.patron.utils;

import static org.folio.edge.core.utils.test.MockOkapi.MOCK_TOKEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.vertx.core.json.JsonObject;
import org.apache.log4j.Logger;
import org.folio.edge.core.utils.test.TestUtils;
import org.folio.edge.patron.model.Hold;
import org.folio.edge.patron.utils.PatronOkapiClient.PatronLookupException;
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
    mockOkapi.close(context);
  }

  @Test
  public void testGetPatronExistent(TestContext context) throws Exception {
    logger.info("=== Test getPatron exists ===");

    client.login("admin", "password").get();
    assertEquals(MOCK_TOKEN, client.getToken());
    String patronId = client.getPatron(PatronMockOkapi.extPatronId).get();
    assertEquals(PatronMockOkapi.patronId, patronId);
  }

  @Test
  public void testGetPatronNonExistentPatron(TestContext context) throws Exception {
    logger.info("=== Test getPatron patron doesn't exist ===");

    client.login("admin", "password").get();
    assertEquals(MOCK_TOKEN, client.getToken());
    try {
      client.getPatron(PatronMockOkapi.extPatronId_notFound).get();
      fail("Expected " + PatronLookupException.class.getName() + " to be thrown");
    } catch (Exception e) {
      if (!(e.getCause() instanceof PatronLookupException)) {
        fail("Expected " + PatronLookupException.class.getName() + " got " + e.getCause().getClass().getName());
        throw e;
      }
    }
  }

  @Test
  public void testGetPatronInsufficientPrivs(TestContext context) throws Exception {
    logger.info("=== Test getPatron patron doesn't exist ===");

    try {
      client.getPatron(PatronMockOkapi.extPatronId).get();
      fail("Expected " + PatronLookupException.class.getName() + " to be thrown");
    } catch (Exception e) {
      if (!(e.getCause() instanceof PatronLookupException)) {
        fail("Expected " + PatronLookupException.class.getName() + " got " + e.getCause().getClass().getName());
        throw e;
      }
    }
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
  public void testCancelHoldSuccessfully(TestContext context) throws Exception {
    logger.info("=== Test cancel hold successfully ===");

    Hold hold = PatronMockOkapi.getHold(PatronMockOkapi.holdCancellationHoldId);
    String holdCancellation = PatronMockOkapi.getHoldCancellation(hold.requestId, patronId);

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.cancelHold(patronId,
          hold.requestId,
          new JsonObject(holdCancellation),
          resp -> resp.bodyHandler(body -> {
            logger.info("mod-patron response body: " + body);
            context.assertEquals(200, resp.statusCode());
            try {
              context.assertEquals(hold, Hold.fromJson(body.toString()));
            }  catch (IOException e) {
              e.printStackTrace();
            }
            async.complete();
          }),
          t -> {
            context.fail(t);
          });
    });
  }

  @Test
  public void testCancelHoldNonExistentPatron(TestContext context) throws Exception {
    logger.info("=== Test removeItemHold patron doesn't exist ===");

    Hold hold = PatronMockOkapi.getHold(PatronMockOkapi.holdCancellationHoldId);

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.cancelHold(PatronMockOkapi.patronId_notFound,
          hold.requestId,
          new JsonObject(PatronMockOkapi.getHoldCancellation(hold.requestId, PatronMockOkapi.patronId_notFound)),
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
  public void testCancelHoldNonExistentHold(TestContext context) throws Exception {
    logger.info("=== Test cancelHold with a hold doesn't exist ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.cancelHold(patronId,
          PatronMockOkapi.holdReqId_notFound,
          new JsonObject(PatronMockOkapi.getHoldCancellation(PatronMockOkapi.holdReqId_notFound, patronId)),
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
  public void testGetRequest(TestContext context) throws Exception {
    logger.info("=== Test successful getRequest ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      client.getRequest(PatronMockOkapi.holdCancellationHoldId,
        resp -> resp.bodyHandler(body -> {
          logger.info("mod-patron response body: " + body);
          context.assertEquals(PatronMockOkapi.getRequest(PatronMockOkapi.holdCancellationHoldId), body.toString());
          async.complete();
        }),
        t -> {
          context.fail(t);
        });
    });
  }

  @Test
  public void testGetRequestNotFound(TestContext context) throws Exception {
    logger.info("=== Test unsuccessful getRequest with unknown requestID ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      String expectedResponse = "request record with ID \"" + PatronMockOkapi.holdReqId_notFound + "\" cannot be found";

      client.getRequest(PatronMockOkapi.holdReqId_notFound,
        resp -> resp.bodyHandler(body -> {
          logger.info("mod-patron response body: " + body);
          context.assertEquals(404, resp.statusCode());
          context.assertEquals(expectedResponse, body.toString());
          async.complete();
        }),
        t -> {
          context.fail(t);
        });
    });
  }
}
