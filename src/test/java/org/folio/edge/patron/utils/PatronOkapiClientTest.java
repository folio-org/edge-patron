package org.folio.edge.patron.utils;

import static org.folio.edge.core.utils.test.MockOkapi.MOCK_TOKEN;
import static org.folio.edge.patron.utils.PatronMockOkapi.PATRON_ID;
import static org.folio.edge.patron.utils.PatronMockOkapi.limit_param;
import static org.folio.edge.patron.utils.PatronMockOkapi.offset_param;
import static org.folio.edge.patron.utils.PatronMockOkapi.patronId_notFound;
import static org.folio.edge.patron.utils.PatronMockOkapi.wrongIntegerParamMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
class PatronOkapiClientTest {

  private static final Logger logger = LogManager.getLogger(PatronOkapiClientTest.class);

  private final String patronId = UUID.randomUUID().toString();
  private final String itemId = UUID.randomUUID().toString();
  private static final String tenant = "diku";
  private static final int reqTimeout = 3000;

  private PatronOkapiClient client;
  private PatronMockOkapi mockOkapi;

  @BeforeEach
  void setUp(VertxTestContext context) {
    int okapiPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(tenant);

    mockOkapi = new PatronMockOkapi(okapiPort, knownTenants);
    mockOkapi.start()
    .onComplete(context.succeedingThenComplete());

    client = spy(new PatronOkapiClient(new OkapiClientFactory(Vertx.vertx(),
      "http://localhost:" + okapiPort, reqTimeout).getOkapiClient(tenant), "alternateTenantId"));
  }

  @AfterEach
  void tearDown(VertxTestContext context) {
    mockOkapi.close()
    .onComplete(context.succeedingThenComplete());
  }

  @Test
  void testGetPatronExistent(VertxTestContext context) throws Exception {
    logger.info("=== Test getPatron exists ===");

    client.login("admin", "password").get();
    assertEquals(MOCK_TOKEN, client.getToken());
    client.getPatron(PatronMockOkapi.extPatronId)
      .onComplete(context.succeeding(pId -> {
        context.verify(() -> assertEquals(PATRON_ID, pId));
        context.completeNow();
      }));
  }

  @Test
  void testGetPatronNonExistentPatron(VertxTestContext context) throws Exception {
    logger.info("=== Test getPatron patron doesn't exist ===");

    client.login("admin", "password").get();
    assertEquals(MOCK_TOKEN, client.getToken());
    client.getPatron(PatronMockOkapi.extPatronId_notFound)
    .onComplete(context.failing(e -> {
      if (!(e instanceof PatronLookupException)) {
        fail("Expected " + PatronLookupException.class.getName() + " got " + e.getClass().getName());
      }
      context.completeNow();
    }));
  }

  @Test
  void testGetPatronExistingSecurePatron(VertxTestContext context) throws Exception {
    logger.info("=== Test getPatron patron doesn't exist in local mod-user but exists in Secure " +
      "tenant's mod-user and accessible through mod-circulation-bff, " +
      "secure requests feature is enabled ===");

    client.login("admin", "password").get();
    assertEquals(MOCK_TOKEN, client.getToken());
    when(client.isSecureRequestsFeatureEnabled()).thenReturn(true);
    client.getPatron(PatronMockOkapi.EXT_SECURE_PATRON_ID)
      .onComplete(context.succeeding(actualPatronId -> {
        assertEquals(PATRON_ID, actualPatronId);
        context.completeNow();
      }));
  }

  @Test
  void testGetPatronNonexistentSecurePatron(VertxTestContext context) throws Exception {
    logger.info("=== Test getPatron patron doesn't exist in both secure and non-secure mod-user," +
      "secure requests feature is enabled ===");

    client.login("admin", "password").get();
    assertEquals(MOCK_TOKEN, client.getToken());
    when(client.isSecureRequestsFeatureEnabled()).thenReturn(true);
    client.getPatron(PatronMockOkapi.EXT_SECURE_PATRON_ID_NOT_FOUND)
      .onComplete(context.failing(e -> {
        if (!(e instanceof PatronLookupException)) {
          fail("Expected " + PatronLookupException.class.getName() + " got " + e.getClass().getName());
        }
        context.completeNow();
      }));
  }

  @Test
  void testGetPatronInsufficientPrivs(VertxTestContext context) {
    logger.info("=== Test getPatron patron doesn't exist ===");

    client.getPatron(PatronMockOkapi.extPatronId)
    .onComplete(context.failing(e -> {
      if (!(e instanceof PatronLookupException)) {
        fail("Expected " + PatronLookupException.class.getName() + " got " + e.getClass().getName());
      }
      context.completeNow();
    }));
  }

  @Test
  void testGetAccountWithAllButBatches(VertxTestContext context) {
    logger.info("=== Test successful getAccount request w/ all data ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      var params = new PatronAccountRequestParams(patronId, true, true, true, false, null, null, null);
      client.getAccount(params,
          resp -> {
            logResponseBody(resp);
            String accountResponse = resp.bodyAsString();
            assertEquals(PatronMockOkapi.getAccountJson(patronId, true, true, true, false), accountResponse);
            try {
              verifyAccountItemsSize(accountResponse, 2);
            } catch (IOException e) {
              logger.error(e.getMessage());
              context.failNow(e);
            }
            context.completeNow();
          },
        context::failNow);
    });
  }

  @Test
  void testGetAccountWithAll(VertxTestContext context) {
    logger.info("=== Test successful getAccount request w/ all data ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      var params = new PatronAccountRequestParams(patronId, true, true, true, true, null, null, null);
      client.getAccount(params,
        resp -> {
          logResponseBody(resp);
          String accountResponse = resp.bodyAsString();
          assertEquals(PatronMockOkapi.getAccountJson(patronId, true, true, true, true), accountResponse);
          try {
            verifyAccountItemsSize(accountResponse, 2);
          } catch (IOException e) {
            logger.error(e.getMessage());
            context.failNow(e);
          }
          context.completeNow();
        },
        context::failNow);
    });
  }

  @Test
  void testGetAccountWithSortedLoans(VertxTestContext context) {
    logger.info("=== Test successful getAccount request w/ sorted loans data ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      var params = new PatronAccountRequestParams(patronId, true, true, true, false, "testSort", null, null);
      client.getAccount(params,
        resp -> {
          logResponseBody(resp);
          try {
            String accountResponse = resp.bodyAsString();
            Account account = Account.fromJson(accountResponse);
            assertEquals(PatronMockOkapi.getAccountWithSortedLoans(patronId), accountResponse);
            assertEquals(PatronMockOkapi.itemId, account.loans.get(0).item.itemId);
            assertEquals(PatronMockOkapi.itemId_overdue, account.loans.get(1).item.itemId);
            context.completeNow();
          } catch (IOException e) {
            context.failNow(e);
          }
        },
        context::failNow);
    });
  }

  @Test
  void testGetAccountWithAllAndLimitEqualsToOne(VertxTestContext context) {
    logger.info("=== Test successful getAccount request w/ all data and limit=1 ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      var params = new PatronAccountRequestParams(patronId, true, true, true, false, null, "1", null);
      client.getAccount(params,
        resp -> {
          logResponseBody(resp);
          String accountResponse = resp.bodyAsString();
          assertEquals(PatronMockOkapi.getAccountWithSingleItemsJson(patronId, true, true, true, false), accountResponse);
          try {
            verifyAccountItemsSize(accountResponse, 1);
          } catch (IOException e) {
            logger.error(e.getMessage());
            context.failNow(e);
          }
          context.completeNow();
        },
        context::failNow);
    });
  }

  @Test
  void testGetAccountLimitIsNegative(VertxTestContext context) {
    logger.info("=== Test getAccount - offset is wrong ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      var params = new PatronAccountRequestParams(PATRON_ID, true, true, true, false, null, null, "-1");
      client.getAccount(params,
        resp -> {
          logResponseBody(resp);
          assertEquals(400, resp.statusCode());
          assertEquals(String.format(wrongIntegerParamMessage, offset_param, "-1"), resp.bodyAsString());
          context.completeNow();
        },
        context::failNow);
    });
  }

  @Test
  void testGetAccountLimitIsWrong(VertxTestContext context) {
    logger.info("=== Test getAccount - limit is wrong ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      var params = new PatronAccountRequestParams(PATRON_ID, true, true, true, false, null, "-1", null);
      client.getAccount(params,
        resp -> {
          logResponseBody(resp);
          assertEquals(400, resp.statusCode());
          assertEquals(String.format(wrongIntegerParamMessage, limit_param, "-1"), resp.bodyAsString());
          context.completeNow();
        },
        context::failNow);
    });
  }

  @Test
  void testGetAccountNotFound(VertxTestContext context) {
    logger.info("=== Test getAccount - patron not found ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      var params = new PatronAccountRequestParams(patronId_notFound, true, true, true, false, null, null, null);
      client.getAccount(params,
        resp -> {
          logResponseBody(resp);
          assertEquals(404, resp.statusCode());
          context.completeNow();
        },
        context::failNow);
    });
  }

  @Test
  void testGetAccountNoCharges(VertxTestContext context) {
    logger.info("=== Test successful getAccount request w/o charges data ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      var params = new PatronAccountRequestParams(patronId, true, true, false, false, null, null, null);
      client.getAccount(params,
        resp -> {
          logResponseBody(resp);
          assertEquals(PatronMockOkapi.getAccountJson(patronId, true, true, false, false), resp.bodyAsString());
          context.completeNow();
        },
        context::failNow);
    });
  }

  @Test
  void testGetAccountNoHolds(VertxTestContext context) {
    logger.info("=== Test successful getAccount request w/o holds data ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      var params = new PatronAccountRequestParams(patronId, true, false, true, false, null, null, null);
      client.getAccount(params,
        resp -> {
          logResponseBody(resp);
          assertEquals(PatronMockOkapi.getAccountJson(patronId, true, false, true, false), resp.bodyAsString());
          context.completeNow();
        },
        context::failNow);
    });
  }

  @Test
  void testGetAccountNoLoans(VertxTestContext context) {
    logger.info("=== Test successful getAccount request w/o loans data ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      var params = new PatronAccountRequestParams(patronId, false, true, true, false, null, null, null);
      client.getAccount(params,
        resp -> {
          logResponseBody(resp);
          assertEquals(PatronMockOkapi.getAccountJson(patronId, false, true, true, false), resp.bodyAsString());
          context.completeNow();
        },
        context::failNow);
    });
  }

  @Test
  void testGetAccountBaseOnly(VertxTestContext context) {
    logger.info("=== Test successful base getAccount request ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      var params = PatronAccountRequestParams.defaultParams(patronId);
      client.getAccount(params,
        resp -> {
          logResponseBody(resp);
          assertEquals(PatronMockOkapi.getAccountJson(patronId, false, false, false, false), resp.bodyAsString());
          context.completeNow();
        },
        context::failNow);
    });
  }

  @Test
  void testGetAccountNoToken(VertxTestContext context) {
    logger.info("=== Test getAccount w/o a token ===");

    var params = PatronAccountRequestParams.defaultParams(patronId);
    client.getAccount(params,
      resp -> {
        logResponseBody(resp);
        assertEquals(403, resp.statusCode());
        context.completeNow();
      },
      context::failNow
    );
  }

  @Test
  void testRenewItemExistent(VertxTestContext context) {
    logger.info("=== Test renewItem exists ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.renewItem(patronId,
          itemId,
          resp -> {
            logResponseBody(resp);
            assertEquals(201, resp.statusCode());
            context.completeNow();
          },
          context::failNow);
    });
  }

  @Test
  void testRenewItemNonExistentItem(VertxTestContext context) {
    logger.info("=== Test renewItem item doesn't exist ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.renewItem(patronId,
          PatronMockOkapi.itemId_notFound,
          resp -> {
            logResponseBody(resp);
            assertEquals(404, resp.statusCode());
            context.completeNow();
          },
         context::failNow);
    });
  }

  @Test
  void testRenewItemNonExistentPatron(VertxTestContext context) {
    logger.info("=== Test renewItem patron doesn't exist ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.renewItem(patronId_notFound,
          itemId,
          resp -> {
            logResponseBody(resp);
            assertEquals(404, resp.statusCode());
            context.completeNow();
          },
          context::failNow);
    });
  }

  @Test
  void testPlaceHoldExistent(VertxTestContext context) throws Exception {
    logger.info("=== Test placeItemHold exists ===");

    Hold hold = PatronMockOkapi.getHold(itemId);
    String holdJSON = hold.toJson();

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.placeItemHold(patronId,
          itemId,
          holdJSON,
          resp -> {
            logResponseBody(resp);
            assertEquals(201, resp.statusCode());
            context.completeNow();
          },
          context::failNow);
    });
  }

  @Test
  void testPlaceHoldNonExistentItem(VertxTestContext context) throws Exception {
    logger.info("=== Test placeItemHold item doesn't exist ===");

    Hold hold = PatronMockOkapi.getHold(PatronMockOkapi.itemId_notFound);
    String holdJSON = hold.toJson();

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.placeItemHold(patronId,
          PatronMockOkapi.itemId_notFound,
          holdJSON,
          resp -> {
            logResponseBody(resp);
            assertEquals(404, resp.statusCode());
            context.completeNow();
          },
          context::failNow);
    });
  }

  @Test
  void testPlaceHoldNonExistentPatron(VertxTestContext context) throws Exception {
    logger.info("=== Test placeItemHold patron doesn't exist ===");

    Hold hold = PatronMockOkapi.getHold(itemId);
    String holdJSON = hold.toJson();

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.placeItemHold(patronId_notFound,
          itemId,
          holdJSON,
          resp -> {
            logResponseBody(resp);
            assertEquals(404, resp.statusCode());
            context.completeNow();
          },
          context::failNow);
    });
  }

  @Test
  void testCancelHoldSuccessfully(VertxTestContext context) {
    logger.info("=== Test cancel hold successfully ===");

    Hold hold = PatronMockOkapi.getHold(PatronMockOkapi.holdCancellationHoldId);
    String holdCancellation = PatronMockOkapi.getHoldCancellation(hold.requestId, patronId);

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.cancelHold(patronId,
          hold.requestId,
          new JsonObject(holdCancellation),
          resp -> {
            logResponseBody(resp);
            assertEquals(200, resp.statusCode());
            try {
              assertEquals(hold, Hold.fromJson(resp.bodyAsString()));
            }  catch (IOException e) {
              e.printStackTrace();
            }
            context.completeNow();
          },
          context::failNow);
    });
  }

  @Test
  void testCancelHoldNonExistentPatron(VertxTestContext context) {
    logger.info("=== Test removeItemHold patron doesn't exist ===");

    Hold hold = PatronMockOkapi.getHold(PatronMockOkapi.holdCancellationHoldId);

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.cancelHold(patronId_notFound,
          hold.requestId,
          new JsonObject(PatronMockOkapi.getHoldCancellation(hold.requestId, patronId_notFound)),
          resp -> {
            logResponseBody(resp);
            assertEquals(404, resp.statusCode());
            context.completeNow();
          },
          context::failNow);
    });
  }

  @Test
  void testCancelHoldNonExistentHold(VertxTestContext context) {
    logger.info("=== Test cancelHold with a hold doesn't exist ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());
      client.cancelHold(patronId,
          PatronMockOkapi.holdReqId_notFound,
          new JsonObject(PatronMockOkapi.getHoldCancellation(PatronMockOkapi.holdReqId_notFound, patronId)),
          resp -> {
            logResponseBody(resp);
            assertEquals(404, resp.statusCode());
            context.completeNow();
          },
          context::failNow);
    });
  }

  @Test
  void testGetRequest(VertxTestContext context) {
    logger.info("=== Test successful getRequest ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      client.getRequest(PatronMockOkapi.holdCancellationHoldId,
        resp -> {
          logResponseBody(resp);
          assertEquals(PatronMockOkapi.getRequest(PatronMockOkapi.holdCancellationHoldId), resp.bodyAsString());
          context.completeNow();
        },
        context::failNow);
    });
  }

  @Test
  void testGetRequestNotFound(VertxTestContext context) {
    logger.info("=== Test unsuccessful getRequest with unknown requestID ===");

    client.login("admin", "password").thenAcceptAsync(v -> {
      assertEquals(MOCK_TOKEN, client.getToken());

      String expectedResponse = "request record with ID \"" + PatronMockOkapi.holdReqId_notFound + "\" cannot be found";

      client.getRequest(PatronMockOkapi.holdReqId_notFound,
        resp -> {
          logResponseBody(resp);
          assertEquals(404, resp.statusCode());
          assertEquals(expectedResponse, resp.bodyAsString());
          context.completeNow();
        },
        context::failNow);
    });
  }

  private void logResponseBody(HttpResponse<Buffer> response) {
    logger.info("mod-patron response body: {}", response.body());
  }

  private void verifyAccountItemsSize(String accountString, int itemsSize) throws IOException {
      Account account = Account.fromJson(accountString);
      assertEquals(itemsSize, account.charges.size());
      assertEquals(itemsSize, account.holds.size());
      assertEquals(itemsSize, account.loans.size());
  }
}
