package org.folio.edge.patron.utils;

import static org.folio.edge.core.utils.test.MockOkapi.MOCK_TOKEN;
import static org.folio.edge.patron.utils.PatronMockOkapi.limit_param;
import static org.folio.edge.patron.utils.PatronMockOkapi.offset_param;
import static org.folio.edge.patron.utils.PatronMockOkapi.wrongIntegerParamMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.OkapiClientFactory;
import org.folio.edge.core.utils.test.TestUtils;
import org.folio.edge.patron.model.Account;
import org.folio.edge.patron.model.Hold;
import org.folio.edge.patron.utils.PatronOkapiClient.PatronLookupException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class PatronOkapiClientTest {

  private static final Logger logger = LogManager.getLogger(PatronOkapiClientTest.class);

  private final String patronId = UUID.randomUUID().toString();
  private final String itemId = UUID.randomUUID().toString();
  private static final String tenant = "diku";
  private static final int reqTimeout = 3000;

  private PatronOkapiClient client;
  private PatronMockOkapi mockOkapi;

  @Before
  public void setUp(TestContext context) throws Exception {
    int okapiPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(tenant);

    mockOkapi = new PatronMockOkapi(okapiPort, knownTenants);
    mockOkapi.start()
    .onComplete(context.asyncAssertSuccess());

    client = spy(new PatronOkapiClient(new OkapiClientFactory(Vertx.vertx(),
      "http://localhost:" + okapiPort, reqTimeout).getOkapiClient(tenant), "alternateTenantId"));
  }

  @After
  public void tearDown(TestContext context) {
    mockOkapi.close()
    .onComplete(context.asyncAssertSuccess());
  }

  @Test
  public void testGetPatronExistent(TestContext context) throws Exception {
    logger.info("=== Test getPatron exists ===");

    client.login("admin", "password").get();
    assertEquals(MOCK_TOKEN, client.getToken());
    client.getPatron(PatronMockOkapi.extPatronId)
    .onComplete(context.asyncAssertSuccess(patronId -> assertEquals(PatronMockOkapi.patronId, patronId)));
  }

  @Test
  public void testGetPatronNonExistentPatron(TestContext context) throws Exception {
    logger.info("=== Test getPatron patron doesn't exist ===");

    client.login("admin", "password").get();
    assertEquals(MOCK_TOKEN, client.getToken());
    client.getPatron(PatronMockOkapi.extPatronId_notFound)
    .onComplete(context.asyncAssertFailure(e -> {
      if (!(e instanceof PatronLookupException)) {
        fail("Expected " + PatronLookupException.class.getName() + " got " + e.getClass().getName());
      }
    }));
  }

  @Test
  public void testGetPatronExistingSecurePatron(TestContext context) throws Exception {
    logger.info("=== Test getPatron patron doesn't exist in local mod-user but exists in Secure " +
      "tenant's mod-user and accessible through mod-circulation-bff, " +
      "secure requests feature is enabled ===");

    client.login("admin", "password").get();
    assertEquals(MOCK_TOKEN, client.getToken());
    when(client.isSecureRequestsFeatureEnabled()).thenReturn(true);
    client.getPatron(PatronMockOkapi.extSecurePatronId)
      .onComplete(context.asyncAssertSuccess(
        patronId -> assertEquals(PatronMockOkapi.patronId, patronId)));
  }

  @Test
  public void testGetPatronNonexistentSecurePatron(TestContext context) throws Exception {
    logger.info("=== Test getPatron patron doesn't exist in both secure and non-secure mod-user," +
      "secure requests feature is enabled ===");

    client.login("admin", "password").get();
    assertEquals(MOCK_TOKEN, client.getToken());
    when(client.isSecureRequestsFeatureEnabled()).thenReturn(true);
    client.getPatron(PatronMockOkapi.extSecurePatronId_notFound)
      .onComplete(context.asyncAssertFailure(e -> {
        if (!(e instanceof PatronLookupException)) {
          fail("Expected " + PatronLookupException.class.getName() + " got " + e.getClass().getName());
        }
      }));
  }

  @Test
  public void testGetPatronInsufficientPrivs(TestContext context) throws Exception {
    logger.info("=== Test getPatron patron doesn't exist ===");

    client.getPatron(PatronMockOkapi.extPatronId)
    .onComplete(context.asyncAssertFailure(e -> {
      if (!(e instanceof PatronLookupException)) {
        fail("Expected " + PatronLookupException.class.getName() + " got " + e.getClass().getName());
      }
    }));
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
          null,
          null,
          null,
          resp -> {
            logger.info("mod-patron response body: " + resp.body());
            String accountResponse = resp.bodyAsString();
            context.assertEquals(PatronMockOkapi.getAccountJson(patronId, true, true, true), accountResponse);
            try {
              verifyAccountItemsSize(accountResponse, 2);
            } catch (IOException e) {
              logger.error(e.getMessage());
              context.fail(e);
            }
            async.complete();
          },
        context::fail);
    });
  }

  @Test
  public void testGetAccountWithSortedLoans(TestContext context) throws Exception {
    logger.info("=== Test successful getAccount request w/ sorted loans data ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      client.getAccount(patronId,
        true,
        true,
        true,
        "testSort",
        null,
        null,
        resp -> {
          logger.info("mod-patron response body: " + resp.body());
          try {
            String accountResponse = resp.bodyAsString();
            Account account = Account.fromJson(accountResponse);
            context.assertEquals(PatronMockOkapi.getAccountWithSortedLoans(patronId), accountResponse);
            context.assertEquals(PatronMockOkapi.itemId, account.loans.get(0).item.itemId);
            context.assertEquals(PatronMockOkapi.itemId_overdue, account.loans.get(1).item.itemId);
            async.complete();
          } catch (IOException e) {
            context.fail(e);
          }
        },
        context::fail);
    });
  }

  @Test
  public void testGetAccountWithAllAndLimitEqualsToOne(TestContext context) throws Exception {
    logger.info("=== Test successful getAccount request w/ all data and limit=1 ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      client.getAccount(patronId,
        true,
        true,
        true,
        null,
        "1",
        null,
        resp -> {
          logger.info("mod-patron response body: " + resp.body());
          String accountResponse = resp.bodyAsString();
          context.assertEquals(PatronMockOkapi.getAccountWithSingleItemsJson(patronId, true, true, true), accountResponse);
          try {
            verifyAccountItemsSize(accountResponse, 1);
          } catch (IOException e) {
            logger.error(e.getMessage());
            context.fail(e);
          }
          async.complete();
        },
        context::fail);
    });
  }

  @Test
  public void testGetAccountLimitIsNegative(TestContext context) throws Exception {
    logger.info("=== Test getAccount - offset is wrong ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      client.getAccount(PatronMockOkapi.patronId,
        true,
        true,
        true,
        null,
        null,
        "-1",
        resp -> {
          logger.info("mod-patron response body: " + resp.body());
          context.assertEquals(400, resp.statusCode());
          context.assertEquals(String.format(wrongIntegerParamMessage, offset_param, "-1"), resp.bodyAsString());
          async.complete();
        },
        context::fail);
    });
  }

  @Test
  public void testGetAccountLimitIsWrong(TestContext context) throws Exception {
    logger.info("=== Test getAccount - limit is wrong ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      client.getAccount(PatronMockOkapi.patronId,
        true,
        true,
        true,
        null,
        "-1",
        null,
        resp -> {
          logger.info("mod-patron response body: " + resp.body());
          context.assertEquals(400, resp.statusCode());
          context.assertEquals(String.format(wrongIntegerParamMessage, limit_param, "-1"), resp.bodyAsString());
          async.complete();
        },
        context::fail);
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
          null,
          null,
          null,
          resp -> {
            logger.info("mod-patron response body: " + resp.body());
            context.assertEquals(404, resp.statusCode());
            async.complete();
          },
        context::fail);
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
          null,
          null,
          null,
          resp -> {
            logger.info("mod-patron response body: " + resp.body());
            context.assertEquals(PatronMockOkapi.getAccountJson(patronId, true, true, false), resp.bodyAsString());
            async.complete();
          },
        context::fail);
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
          null,
          null,
          null,
          resp -> {
            logger.info("mod-patron response body: " + resp.body());
            context.assertEquals(PatronMockOkapi.getAccountJson(patronId, true, false, true), resp.bodyAsString());
            async.complete();
          },
        context::fail);
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
          null,
          null,
          null,
          resp -> {
            logger.info("mod-patron response body: " + resp.body());
            context.assertEquals(PatronMockOkapi.getAccountJson(patronId, false, true, true), resp.bodyAsString());
            async.complete();
          },
        context::fail);
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
          null,
          null,
          null,
          resp -> {
            logger.info("mod-patron response body: " + resp.body());
            context.assertEquals(PatronMockOkapi.getAccountJson(patronId, false, false, false), resp.bodyAsString());
            async.complete();
          },
        context::fail);
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
        null,
        null,
        null,
        resp -> {
          logger.info("mod-patron response body: " + resp.body());
          context.assertEquals(403, resp.statusCode());
          async.complete();
        },
        context::fail);
  }

  @Test
  public void testRenewItemExistent(TestContext context) {
    logger.info("=== Test renewItem exists ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.renewItem(patronId,
          itemId,
          resp -> {
            logger.info("mod-patron response body: " + resp.body());
            context.assertEquals(201, resp.statusCode());
            async.complete();
          },
          context::fail);
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
          resp -> {
            logger.info("mod-patron response body: " + resp.body());
            context.assertEquals(404, resp.statusCode());
            async.complete();
          },
         context::fail);
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
          resp -> {
            logger.info("mod-patron response body: " + resp.body());
            context.assertEquals(404, resp.statusCode());
            async.complete();
          },
          context::fail);
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
          resp -> {
            logger.info("mod-patron response body: " + resp.body());
            context.assertEquals(201, resp.statusCode());
            async.complete();
          },
          context::fail);
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
          resp -> {
            logger.info("mod-patron response body: " + resp.body());
            context.assertEquals(404, resp.statusCode());
            async.complete();
          },
          context::fail);
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
          resp -> {
            logger.info("mod-patron response body: " + resp.body());
            context.assertEquals(404, resp.statusCode());
            async.complete();
          },
          context::fail);
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
          resp -> {
            logger.info("mod-patron response body: " + resp.body());
            context.assertEquals(200, resp.statusCode());
            try {
              context.assertEquals(hold, Hold.fromJson(resp.bodyAsString()));
            }  catch (IOException e) {
              e.printStackTrace();
            }
            async.complete();
          },
          context::fail);
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
          resp -> {
            logger.info("mod-patron response body: " + resp.body());
            context.assertEquals(404, resp.statusCode());
            async.complete();
          },
          context::fail);
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
          resp -> {
            logger.info("mod-patron response body: " + resp.body());
            context.assertEquals(404, resp.statusCode());
            async.complete();
          },
          context::fail);
    });
  }

  @Test
  public void testGetRequest(TestContext context) throws Exception {
    logger.info("=== Test successful getRequest ===");

    Async async = context.async();
    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      client.getRequest(PatronMockOkapi.holdCancellationHoldId,
        resp -> {
          logger.info("mod-patron response body: " + resp.body());
          context.assertEquals(PatronMockOkapi.getRequest(PatronMockOkapi.holdCancellationHoldId), resp.bodyAsString());
          async.complete();
        },
        context::fail);
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
        resp -> {
          logger.info("mod-patron response body: " + resp.body());
          context.assertEquals(404, resp.statusCode());
          context.assertEquals(expectedResponse, resp.bodyAsString());
          async.complete();
        },
        context::fail);
    });
  }

  private void verifyAccountItemsSize(String accountString, int itemsSize) throws IOException {
      Account account = Account.fromJson(accountString);
      assertEquals(itemsSize, account.charges.size());
      assertEquals(itemsSize, account.holds.size());
      assertEquals(itemsSize, account.loans.size());
  }
}
