package org.folio.edge.patron;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.DAY_IN_MILLIS;
import static org.folio.edge.core.Constants.SYS_LOG_LEVEL;
import static org.folio.edge.core.Constants.SYS_OKAPI_URL;
import static org.folio.edge.core.Constants.SYS_PORT;
import static org.folio.edge.core.Constants.SYS_REQUEST_TIMEOUT_MS;
import static org.folio.edge.core.Constants.SYS_RESPONSE_COMPRESSION;
import static org.folio.edge.core.Constants.SYS_SECURE_STORE_PROP_FILE;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.patron.Constants.MSG_ACCESS_DENIED;
import static org.folio.edge.patron.Constants.MSG_REQUEST_TIMEOUT;
import static org.folio.edge.patron.Constants.MSG_HOLD_NOBODY;
import static org.folio.edge.patron.utils.PatronMockOkapi.holdCancellationHoldId;
import static org.folio.edge.patron.utils.PatronMockOkapi.holdReqId_notFound;
import static org.folio.edge.patron.utils.PatronMockOkapi.holdReqTs;
import static org.folio.edge.patron.utils.PatronMockOkapi.instanceId_notFound;
import static org.folio.edge.patron.utils.PatronMockOkapi.invalidHoldCancellationdHoldId;
import static org.folio.edge.patron.utils.PatronMockOkapi.itemId_notFound;
import static org.folio.edge.patron.utils.PatronMockOkapi.limit_param;
import static org.folio.edge.patron.utils.PatronMockOkapi.malformedHoldCancellationHoldId;
import static org.folio.edge.patron.utils.PatronMockOkapi.nonUUIDHoldCanceledByPatronId;
import static org.folio.edge.patron.utils.PatronMockOkapi.offset_param;
import static org.folio.edge.patron.utils.PatronMockOkapi.readMockFile;
import static org.folio.edge.patron.utils.PatronMockOkapi.wrongIntegerParamMessage;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.ApiKeyUtils;
import org.folio.edge.core.utils.test.TestUtils;
import org.folio.edge.patron.model.Account;
import org.folio.edge.patron.model.Patron;
import org.folio.edge.patron.model.error.ErrorMessage;
import org.folio.edge.patron.model.Hold;
import org.folio.edge.patron.model.Loan;
import org.folio.edge.patron.utils.PatronMockOkapi;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.restassured.RestAssured;
import io.restassured.config.DecoderConfig;
import io.restassured.config.DecoderConfig.ContentDecoder;
import io.restassured.response.Response;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {

  private static final Logger logger = LogManager.getLogger(MainVerticleTest.class);
  private static final String extPatronId = PatronMockOkapi.extPatronId;
  private static final String patronId = PatronMockOkapi.patronId;
  private static final String itemId = UUID.randomUUID().toString();
  private static final String instanceId = UUID.randomUUID().toString();
  private static final String holdId = UUID.randomUUID().toString();
  private static final String apiKey = ApiKeyUtils.generateApiKey(10, "diku", "diku");
  private static final String badApiKey = apiKey + "0000";
  private static final String unknownTenantApiKey = ApiKeyUtils.generateApiKey(10, "bogus", "diku");

  private static final long requestTimeoutMs = 3000L;

  private static Vertx vertx;
  private static PatronMockOkapi mockOkapi;

  @BeforeClass
  public static void setUpOnce(TestContext context) throws Exception {
    int okapiPort = TestUtils.getPort();
    int serverPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(ApiKeyUtils.parseApiKey(apiKey).tenantId);

    vertx = Vertx.vertx();

    System.setProperty(SYS_PORT, String.valueOf(serverPort));
    System.setProperty(SYS_OKAPI_URL, "http://localhost:" + okapiPort);
    System.setProperty(SYS_SECURE_STORE_PROP_FILE, "src/main/resources/ephemeral.properties");
    System.setProperty(SYS_LOG_LEVEL, "DEBUG");
    System.setProperty(SYS_RESPONSE_COMPRESSION, "true");
    System.setProperty(SYS_REQUEST_TIMEOUT_MS, String.valueOf(requestTimeoutMs));

    mockOkapi = spy(new PatronMockOkapi(okapiPort, knownTenants));
    mockOkapi.start()
    .compose(x -> {
      final DeploymentOptions opt = new DeploymentOptions();
      return vertx.deployVerticle(MainVerticle.class.getName(), opt);
    })
    .onComplete(context.asyncAssertSuccess());

    RestAssured.baseURI = "http://localhost:" + serverPort;
    RestAssured.port = serverPort;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @AfterClass
  public static void tearDownOnce(TestContext context) {
    logger.info("Shutting down server");
    mockOkapi.close()
    .compose(x -> vertx.close())
    .onSuccess(x -> logger.info("Successfully shut down mock Okapi and edge-patron server"))
    .onComplete(context.asyncAssertSuccess());
  }

  @After
  public void after() {
    mockOkapi.setDelay(0);
  }

  @Test
  public void testAdminHealth(TestContext context) {
    logger.info("=== Test the health check endpoint ===");

    final Response resp = RestAssured
      .get("/admin/health")
      .then()
      .contentType(TEXT_PLAIN)
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
      .extract()
      .response();

    assertEquals("\"OK\"", resp.body().asString());
  }

  @Test
  public void testGetAccountUnknownApiKey(TestContext context) throws Exception {
    logger.info("=== Test getAccount with unknown apiKey (tenant) ===");

    int expectedStatusCode = 401;

    final Response resp = RestAssured
      .get(String.format("/patron/account/%s?apikey=%s", extPatronId, unknownTenantApiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals(MSG_ACCESS_DENIED, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testGetAccountBadApiKey(TestContext context) throws Exception {
    logger.info("=== Test getAccount with malformed apiKey ===");

    int expectedStatusCode = 401;

    final Response resp = RestAssured
      .get(String.format("/patron/account/%s?apikey=%s", extPatronId, badApiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

    assertEquals(MSG_ACCESS_DENIED, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testGetAccountPatronFound(TestContext context) throws Exception {
    logger.info("=== Test request where patron is found ===");

    final String expected = PatronMockOkapi.getAccountJson(patronId, false, false, false);

    RestAssured
    .get(String.format("/patron/account/%s?apikey=%s", extPatronId, apiKey))
    .then()
    .statusCode(200)
    .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
    .body(is(expected));
  }

  @Test
  public void testGetExternalLCPatrons(TestContext context) {
    logger.info("=== Test get external patron ===");
    int expectedStatusCode = 200;
    RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .get(
        String.format("/patron/account/%s/external-patrons?apikey=%s&expired=false",UUID.randomUUID(), apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();
  }

  @Test
  public void testGetAccountPatronFoundGzip(TestContext context) throws Exception {
    logger.info("=== Patron in GZip compression ===");

    final String expected = PatronMockOkapi.getAccountJson(patronId, false, false, false);

    RestAssured.given()
      .config(RestAssured.config().decoderConfig(new DecoderConfig(ContentDecoder.GZIP)))
    .when()
      .get(String.format("/patron/account/%s?apikey=%s", extPatronId, apiKey))
    .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .header(HttpHeaders.CONTENT_ENCODING, "gzip")
      .body(is(expected));
  }

  @Test
  public void testGetAccountPatronFoundDeflate(TestContext context) throws Exception {
    logger.info("=== Patron in Deflate compression ===");

    final String expected = PatronMockOkapi.getAccountJson(patronId, false, false, false);

    RestAssured.given()
      .config(RestAssured.config().decoderConfig(new DecoderConfig(ContentDecoder.DEFLATE)))
    .when()
      .get(String.format("/patron/account/%s?apikey=%s", extPatronId, apiKey))
    .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .header(HttpHeaders.CONTENT_ENCODING, "deflate")
      .body(is(expected));
  }

  @Test
  public void testGetAccountPatronNotFound(TestContext context) throws Exception {
    logger.info("=== Test request where patron isn't found ===");

    int expectedStatusCode = 404;

    final Response resp = RestAssured
      .get(String.format("/patron/account/%s?apikey=%s", PatronMockOkapi.extPatronId_notFound, apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

    assertEquals("Unable to find patron " + PatronMockOkapi.extPatronId_notFound, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testGetAccountByEmail(TestContext context) {
    logger.info("=== Test request for getting external_patron by email ===");

    RestAssured
      .get(String.format("/patron/account/%s/by-email/%s?apikey=%s", extPatronId, "fgh@mail", apiKey))
      .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
  }

  @Test
  public void testGetAccountNoApiKey(TestContext context) throws Exception {
    logger.info("=== Test request with malformed apiKey ===");

    int expectedStatusCode = 401;

    final Response resp = RestAssured
      .get(String.format("/patron/account/%s", patronId))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

      ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

    assertEquals(MSG_ACCESS_DENIED, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testGetAccountPatronFoundIncludeLoans(TestContext context) throws Exception {
    logger.info("=== Test getAccount/includeLoans ===");

    final Response resp = RestAssured
      .get(String.format("/patron/account/%s?apikey=%s&includeLoans=true", patronId, apiKey))
      .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    String expected = PatronMockOkapi.getAccountJson(patronId, true, false, false);
    String actual = resp.body().asString();

    assertEquals(expected, actual);
  }

  @Test
  public void testGetAccountPatronFoundIncludeCharges(TestContext context) throws Exception {
    logger.info("=== Test getAccount/includeCharges ===");

    final Response resp = RestAssured
      .get(String.format("/patron/account/%s?apikey=%s&includeCharges=true", patronId, apiKey))
      .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    String expected = PatronMockOkapi.getAccountJson(patronId, false, true, false);
    String actual = resp.body().asString();

    assertEquals(expected, actual);
  }

  @Test
  public void testGetAccountPatronFoundIncludeHolds(TestContext context) throws Exception {
    logger.info("=== Test getAccount/includeHolds ===");

    final Response resp = RestAssured
      .get(String.format("/patron/account/%s?apikey=%s&includeHolds=true", patronId, apiKey))
      .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    String expected = PatronMockOkapi.getAccountJson(patronId, false, false, true);
    String actual = resp.body().asString();

    assertEquals(expected, actual);
  }

  @Test
  public void testGetAccountPatronFoundIncludeAll(TestContext context) throws Exception {
    logger.info("=== Test getAccount/includeLoans,includeCharges,includeHolds ===");

    final Response resp = RestAssured
      .get(String.format("/patron/account/%s?apikey=%s&includeLoans=true&includeHolds=true&includeCharges=true",
          patronId, apiKey))
      .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    String expected = PatronMockOkapi.getAccountJson(patronId, true, true, true);
    String actual = resp.body().asString();

    assertEquals(expected, actual);
  }

  @Test
  public void testGetAccountPatronFoundIncludeAllAndLimitEqualsToOne(TestContext context) throws Exception {
    logger.info("=== Test getAccount/includeLoans,includeCharges,includeHolds & limit=1 ===");

    final Response resp = RestAssured
      .get(String.format("/patron/account/%s?apikey=%s&includeLoans=true&includeHolds=true&includeCharges=true&limit=1&offset=0",
        patronId, apiKey))
      .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    String expected = PatronMockOkapi.getAccountWithSingleItemsJson(patronId, true, true, true);
    String actual = resp.body().asString();

    assertEquals(expected, actual);
  }

  @Test
  public void testGetAccountPatronFoundIncludeLoansAndSortByAndNegativeLimit(TestContext context) throws Exception {
    logger.info("=== Test getAccount/includeLoans & sortBy & negative limit ===");

    int expectedStatusCode = 400;
    final Response resp = RestAssured
      .get(String.format("/patron/account/%s?apikey=%s&includeLoans=true&includeHolds=false&includeCharges=false&sortBy=testSort&limit=-1",
        patronId, apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals(String.format(wrongIntegerParamMessage, limit_param, "-1"), msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testGetAccountOffsetIsNegative(TestContext context) throws Exception {
    logger.info("=== Test getAccount offset is negative ===");

    int expectedStatusCode = 400;
    final Response resp = RestAssured
      .get(String.format("/patron/account/%s?apikey=%s&includeLoans=true&includeHolds=true&includeCharges=true&limit=1&offset=-1",
        patronId, apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals(String.format(wrongIntegerParamMessage, offset_param, "-1"), msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testGetAccountOffsetIsNotNumber(TestContext context) throws Exception {
    logger.info("=== Test getAccount offset is not a number ===");

    int expectedStatusCode = 400;
    final Response resp = RestAssured
      .get(String.format("/patron/account/%s?apikey=%s&includeLoans=true&includeHolds=true&includeCharges=true&limit=1&offset=test",
        patronId, apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals(String.format(wrongIntegerParamMessage, offset_param, "test"), msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testGetAccountLimitIsNotNumber(TestContext context) throws Exception {
    logger.info("=== Test getAccount limit is not a number ===");

    int expectedStatusCode = 400;
    final Response resp = RestAssured
      .get(String.format("/patron/account/%s?apikey=%s&includeLoans=true&includeHolds=true&includeCharges=true&limit=test",
        patronId, apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals(String.format(wrongIntegerParamMessage, limit_param, "test"), msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testGetAccountEmptyQueryArgs(TestContext context) throws Exception {
    logger.info("=== Test getAccount with empty query args ===");

    final Response resp = RestAssured
      .get(
          String.format("/patron/account/%s?apikey=%s&includeCharges=&includeLoans=&includeCharges=", patronId, apiKey))
      .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    String expected = PatronMockOkapi.getAccountJson(patronId, false, false, false);
    String actual = resp.body().asString();

    assertEquals(expected, actual);
  }

  @Test
  public void testGetAccountRequestTimeout(TestContext context) throws Exception {
    logger.info("=== Test getAccount request timeout ===");

    mockOkapi.setDelay(requestTimeoutMs * 3);
    RestAssured
      .get(String.format("/patron/account/%s?apikey=%s", patronId, apiKey))
      .then()
      .contentType(APPLICATION_JSON)
      .statusCode(408)
      .body("code", is(408))
      .body("errorMessage", is(MSG_REQUEST_TIMEOUT));
  }

  @Test
  public void testRenewSuccess(TestContext context) throws Exception {
    logger.info("=== Test successfulrenewal ===");

    final Response resp = RestAssured
      .post(
          String.format("/patron/account/%s/item/%s/renew?apikey=%s", patronId, itemId, apiKey))
      .then()
      .statusCode(201)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    Loan expected = Loan.fromJson(PatronMockOkapi.getLoanJson(patronId, itemId));
    Loan actual = Loan.fromJson(resp.body().asString());

    assertEquals(expected, actual);
  }

  @Test
  public void testRenewPatronNotFound(TestContext context) throws Exception {
    logger.info("=== Test renew w/ patron not found ===");

    int expectedStatusCode = 404;

    final Response resp = RestAssured
      .post(
          String.format("/patron/account/%s/item/%s/renew?apikey=%s", PatronMockOkapi.extPatronId_notFound, itemId,
              apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals("Unable to find patron " + PatronMockOkapi.extPatronId_notFound, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testRenewItemNotFound(TestContext context) throws Exception {
    logger.info("=== Test renew w/ item not found ===");

    int expectedStatusCode = 404;

    final Response resp = RestAssured
      .post(
          String.format("/patron/account/%s/item/%s/renew?apikey=%s", extPatronId, PatronMockOkapi.itemId_notFound,
              apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals(PatronMockOkapi.itemId_notFound + " not found", msg.message);
    assertEquals(expectedStatusCode,  msg.httpStatusCode);
  }

  @Test
  public void testRenewUnknownApiKey(TestContext context) throws Exception {
    logger.info("=== Test renew with unknown apiKey (tenant) ===");

    int expectedStatusCode = 401;

    final Response resp = RestAssured
      .post(String.format("/patron/account/%s/item/%s/renew?apikey=%s", patronId, itemId, unknownTenantApiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

      ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

    assertEquals(MSG_ACCESS_DENIED, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testRenewBadApiKey(TestContext context) throws Exception {
    logger.info("=== Test renew with malformed apiKey ===");

    final Response resp = RestAssured
      .post(String.format("/patron/account/%s/item/%s/renew?apikey=%s", patronId, itemId, badApiKey))
      .then()
      .statusCode(401)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

    assertEquals(MSG_ACCESS_DENIED, msg.message);
    assertEquals(401, msg.httpStatusCode);
  }

  @Test
  public void testRenewRequestTimeout(TestContext context) throws Exception {
    logger.info("=== Test renew request timeout ===");

    mockOkapi.setDelay(requestTimeoutMs * 3);
    RestAssured
      .post(String.format("/patron/account/%s/item/%s/renew?apikey=%s", patronId, itemId,
          apiKey))
      .then()
      .contentType(APPLICATION_JSON)
      .statusCode(408)
      .body("code", is(408))
      .body("errorMessage", is(MSG_REQUEST_TIMEOUT));
  }

  @Test
  public void testRenewRequesMaxRenewal(TestContext context) throws Exception {
      logger.info("=== Test MAX Renewal ===");

      int expectedStatusCode = 422;

      final Response resp = RestAssured
              .with()
              .post(String.format("/patron/account/%s/item/%s/renew?apikey=%s", patronId, PatronMockOkapi.itemId_reached_max_renewals,
                      apiKey))
              .then()
              .contentType(APPLICATION_JSON)
              .statusCode(expectedStatusCode)
              .extract()
              .response();

      ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

      assertEquals("loan has reached its maximum number of renewals", msg.message);
      assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testRenewRequesMaxRenewalWithEmptyErrors(TestContext context) throws Exception {
      logger.info("=== Test MAX Renewal With Empty Errors ===");

      int expectedStatusCode = 422;

      final Response resp = RestAssured
              .with()
              .post(String.format("/patron/account/%s/item/%s/renew?apikey=%s", patronId, PatronMockOkapi.itemId_reached_max_renewals_empty_error_msg,
                      apiKey))
              .then()
              .contentType(APPLICATION_JSON)
              .statusCode(expectedStatusCode)
              .extract()
              .response();

      ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

      assertEquals("No error message found", msg.message);
      assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testRenewRequesMaxRenewalWithBadJson(TestContext context) throws Exception {
      logger.info("=== Test MAX Renewal With Bad Json ===");

      int expectedStatusCode = 422;

      final Response resp = RestAssured
              .with()
              .post(String.format("/patron/account/%s/item/%s/renew?apikey=%s", patronId, PatronMockOkapi.itemId_reached_max_renewals_bad_json_msg,
                      apiKey))
              .then()
              .contentType(APPLICATION_JSON)
              .statusCode(expectedStatusCode)
              .extract()
              .response();

      ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

      assertEquals("A problem encountered when extracting error message", msg.message);
      assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testPlaceInstanceHoldSuccess(TestContext context) throws Exception {
    logger.info("=== Test place instance hold success ===");

    Hold hold = PatronMockOkapi.getHold(instanceId);

    final Response resp = RestAssured
      .with()
      .body(hold.toJson())
      .contentType(APPLICATION_JSON)
      .post(
          String.format("/patron/account/%s/instance/%s/hold?apikey=%s", patronId, instanceId, apiKey))
      .then()
      .statusCode(201)
      .extract()
      .response();

    Hold expected = Hold.fromJson(PatronMockOkapi.getPlacedHoldJson(hold));
    Hold actual = Hold.fromJson(resp.body().asString());
    validateHolds(expected, actual);
  }

  @Test
  public void testPlaceInstanceHoldPatronNotFound(TestContext context) throws Exception {
    logger.info("=== Test place instance hold w/ patron not found ===");

    Hold hold = PatronMockOkapi.getHold(instanceId);
    int expectedStatusCode = 404;

    final Response resp = RestAssured
      .with()
      .body(hold.toJson())
      .contentType(APPLICATION_JSON)
      .post(
          String.format("/patron/account/%s/instance/%s/hold?apikey=%s", PatronMockOkapi.extPatronId_notFound,
              instanceId,
              apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals("Unable to find patron " + PatronMockOkapi.extPatronId_notFound, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testPostExternalLCPatron(TestContext context) throws Exception {
    logger.info("=== Test post external patron ===");

    Patron patron = PatronMockOkapi.getPatron();
    int expectedStatusCode = 201;
    RestAssured
      .with()
      .body(patron.toJson())
      .contentType(APPLICATION_JSON)
      .post(
        String.format("/patron/account/%s?apikey=%s", UUID.randomUUID(), apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();
  }

  @Test
  public void testPutExternalLCPatron(TestContext context) throws Exception {
    logger.info("=== Test put external patron ===");

    Patron patron = PatronMockOkapi.getPatron();
    int expectedStatusCode = 204;
    RestAssured
      .with()
      .body(patron.toJson())
      .contentType(APPLICATION_JSON)
      .put(
        String.format("/patron/account/%s/by-email/%s?apikey=%s", UUID.randomUUID(), "TestMail", apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
  }

  @Test
  public void testPutExternalLCPatronWithEmptyBody(TestContext context) {
    logger.info("=== Test put external patron ===");

    int expectedStatusCode = 400;
    RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .put(
        String.format("/patron/account/%s/by-email/%s?apikey=%s", UUID.randomUUID(), "TestMail", apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();
  }

  @Test
  public void testPlaceInstanceHoldInstanceNotFound(TestContext context) throws Exception {
    logger.info("=== Test place instance hold w/ instance not found ===");

    Hold hold = PatronMockOkapi.getHold(PatronMockOkapi.instanceId_notFound);
    int expectedStatusCode = 404;

    final Response resp = RestAssured
      .with()
      .body(hold.toJson())
      .contentType(APPLICATION_JSON)
      .post(
          String.format("/patron/account/%s/instance/%s/hold?apikey=%s", patronId, PatronMockOkapi.instanceId_notFound,
              apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals(expectedStatusCode, msg.httpStatusCode);
    assertTrue(Optional.ofNullable(msg.message).orElse(StringUtils.EMPTY).contains(PatronMockOkapi.instanceId_notFound));
  }

  @Test
  public void testPlaceInstanceHoldUnknownApiKey(TestContext context) throws Exception {
    logger.info("=== Test place instance hold with unknown apiKey (tenant) ===");

    Hold hold = PatronMockOkapi.getHold(instanceId);
    int statusCode = 401;

    final Response resp = RestAssured
      .with()
      .body(hold.toJson())
      .contentType(APPLICATION_JSON)
      .post(
          String.format("/patron/account/%s/instance/%s/hold?apikey=%s", patronId, instanceId, unknownTenantApiKey))
      .then()
      .statusCode(statusCode)
      .extract()
      .response();

      ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

      assertEquals(MSG_ACCESS_DENIED, msg.message);
      assertEquals(statusCode, msg.httpStatusCode);
  }

  @Test
  public void testPlaceInstanceHoldBadApiKey(TestContext context) throws Exception {
    logger.info("=== Test place instance hold with malformed apiKey ===");

    Hold hold = PatronMockOkapi.getHold(instanceId);
    int expectedStatusCode = 401;

    final Response resp = RestAssured
      .with()
      .body(hold.toJson())
      .contentType(APPLICATION_JSON)
      .post(
          String.format("/patron/account/%s/instance/%s/hold?apikey=%s", patronId, instanceId, badApiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

    assertEquals(MSG_ACCESS_DENIED, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testPlaceInstanceHoldRequestTimeout(TestContext context) throws Exception {
    logger.info("=== Test place instance hold request timeout ===");

    Hold hold = PatronMockOkapi.getHold(instanceId);

    mockOkapi.setDelay(requestTimeoutMs * 3);
    RestAssured
      .with()
      .body(hold.toJson())
      .contentType(APPLICATION_JSON)
      .post(
          String.format("/patron/account/%s/instance/%s/hold?apikey=%s", patronId, instanceId,
              apiKey))
      .then()
      .contentType(APPLICATION_JSON)
      .statusCode(408)
      .body("code", is(408))
      .body("errorMessage", is(MSG_REQUEST_TIMEOUT));
  }

  @Test
  public void testPlaceInstanceHoldNoBody(TestContext context) throws Exception {
    logger.info("=== Test place instance hold request with no request body ===");

    mockOkapi.setDelay(requestTimeoutMs * 3);
    RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .post(
          String.format("/patron/account/%s/instance/%s/hold?apikey=%s", patronId, instanceId,
              apiKey))
      .then()
      .contentType(APPLICATION_JSON)
      .statusCode(400)
      .body("code", is(400))
      .body("errorMessage", is(MSG_HOLD_NOBODY));
  }

  @Test
  public void testPlaceItemHoldSuccess(TestContext context) throws Exception {
    logger.info("=== Test successful item hold ===");

    Hold hold = PatronMockOkapi.getHold(itemId, new Date(holdReqTs));

    final Response resp = RestAssured
      .with()
      .body(hold.toJson())
      .contentType(APPLICATION_JSON)
      .post(
          String.format("/patron/account/%s/item/%s/hold?apikey=%s", patronId, itemId, apiKey))
      .then()
      .statusCode(201)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    Hold expected = Hold.fromJson(PatronMockOkapi.getPlacedHoldJson(hold));
    Hold actual = Hold.fromJson(resp.body().asString());
    validateHolds(expected, actual);
  }

  @Test
  public void testPlaceItemHoldWithoutRequestDateSuccess(TestContext context) throws Exception {
    logger.info("=== Test successful item hold ===");

    Hold hold = PatronMockOkapi.getHold(itemId, null);

    final Response resp = RestAssured
      .with()
      .body(hold.toJson())
      .contentType(APPLICATION_JSON)
      .post(
        String.format("/patron/account/%s/item/%s/hold?apikey=%s", patronId, itemId, apiKey))
      .then()
      .statusCode(201)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    Hold expected = Hold.fromJson(PatronMockOkapi.getPlacedHoldJson(hold));
    Hold actual = Hold.fromJson(resp.body().asString());
    validateHolds(expected, actual);
  }

  @Test
  public void testPlaceItemHoldWithInvalidExpirationDateSuccess(TestContext context) throws Exception {
    logger.info("=== Test successful item hold ===");

    Hold hold = PatronMockOkapi.getHold(itemId);

    //alter the hold's expiration date to make it invalid
    String holdJson = swapExpirationDate(hold.toJson(), ": \"0001-01-01T00:00:00\"");

    final Response resp = RestAssured
      .with()
      .body(holdJson)
      .contentType(APPLICATION_JSON)
      .post(
        String.format("/patron/account/%s/item/%s/hold?apikey=%s", patronId, itemId, apiKey))
      .then()
      .statusCode(201)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    Hold expected = Hold.fromJson(PatronMockOkapi.getPlacedHoldJson(hold));
    Hold actual = Hold.fromJson(resp.body().asString());
    validateHolds(expected, actual);

    assertNull(actual.expirationDate);
  }

  @Test
  public void testPlaceItemHoldWithValidExpirationDateSuccess(TestContext context) throws Exception {
    logger.info("=== Test successful item hold ===");

    Hold hold = PatronMockOkapi.getHold(itemId);

    //alter the hold's expiration date to a date that EDS would send
    String holdJson = swapExpirationDate(hold.toJson(), ": \"2019-09-20T10:00:00.000+0000\"");

    final Response resp = RestAssured
      .with()
      .body(holdJson)
      .contentType(APPLICATION_JSON)
      .post(
        String.format("/patron/account/%s/item/%s/hold?apikey=%s", patronId, itemId, apiKey))
      .then()
      .statusCode(201)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    Hold expected = Hold.fromJson(PatronMockOkapi.getPlacedHoldJson(hold));
    Hold actual = Hold.fromJson(resp.body().asString());
    validateHolds(expected, actual);

    assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse("2019-09-20T10:00:00.000+0000"), actual.expirationDate);
  }

  @Test
  public void testPlaceItemHoldPatronNotFound(TestContext context) throws Exception {
    logger.info("=== Test place item hold w/ patron not found ===");

    Hold hold = PatronMockOkapi.getHold(itemId);
    int expectedStatusCode = 404;

    final Response resp = RestAssured
      .with()
      .body(hold.toJson())
      .contentType(APPLICATION_JSON)
      .post(
          String.format("/patron/account/%s/item/%s/hold?apikey=%s", PatronMockOkapi.extPatronId_notFound, itemId,
              apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals("Unable to find patron " + PatronMockOkapi.extPatronId_notFound, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testPlaceItemHoldItemNotFound(TestContext context) throws Exception {
    logger.info("=== Test place item hold w/ item not found ===");

    Hold hold = PatronMockOkapi.getHold(PatronMockOkapi.itemId_notFound);
    int expectedStatusCode = 404;

    final Response resp = RestAssured
      .with()
      .body(hold.toJson())
      .contentType(APPLICATION_JSON)
      .post(
          String.format("/patron/account/%s/item/%s/hold?apikey=%s", patronId, PatronMockOkapi.itemId_notFound, apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals(PatronMockOkapi.itemId_notFound + " not found", msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testPlaceItemHoldUnknownApiKey(TestContext context) throws Exception {
    logger.info("=== Test place item hold with unknown apiKey (tenant) ===");

    Hold hold = PatronMockOkapi.getHold(itemId);
    int expectedStatusCode = 401;

    final Response resp = RestAssured
      .with()
      .body(hold.toJson())
      .contentType(APPLICATION_JSON)
      .post(
          String.format("/patron/account/%s/item/%s/hold?apikey=%s", patronId, itemId, unknownTenantApiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

      ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

    assertEquals(MSG_ACCESS_DENIED, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testPlaceItemHoldBadApiKey(TestContext context) throws Exception {
    logger.info("=== Test place item hold with malformed apiKey ===");

    Hold hold = PatronMockOkapi.getHold(itemId);
    int expectedStatusCode = 401;

    final Response resp = RestAssured
      .with()
      .body(hold.toJson())
      .contentType(APPLICATION_JSON)
      .post(
          String.format("/patron/account/%s/item/%s/hold?apikey=%s", patronId, itemId, badApiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

    assertEquals(MSG_ACCESS_DENIED, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testPlaceItemHoldRequestTimeout(TestContext context) throws Exception {
    logger.info("=== Test place item hold request timeout ===");

    Hold hold = PatronMockOkapi.getHold(itemId);

    mockOkapi.setDelay(requestTimeoutMs * 3);
    RestAssured
      .with()
      .body(hold.toJson())
      .contentType(APPLICATION_JSON)
      .post(
          String.format("/patron/account/%s/item/%s/hold?apikey=%s", patronId, itemId,
              apiKey))
      .then()
      .contentType(APPLICATION_JSON)
      .statusCode(408)
      .body("code", is(408))
      .body("errorMessage", is(MSG_REQUEST_TIMEOUT));
  }

  @Test
  public void testPlaceItemHoldRequestNoBody(TestContext context) throws Exception {
    logger.info("=== Test place item hold request with no request body ===");

    RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .post(
          String.format("/patron/account/%s/item/%s/hold?apikey=%s", patronId, itemId,
              apiKey))
      .then()
      .contentType(APPLICATION_JSON)
      .statusCode(400)
      .body("code", is(400))
      .body("errorMessage", is(MSG_HOLD_NOBODY));
  }

  @Test
  public void testAllowedServicePointsSuccess(TestContext context) throws Exception {
    logger.info("=== Test successful allowed service points request ===");

    final Response resp = RestAssured
      .with()
      .get(String.format("/patron/account/%s/instance/%s/allowed-service-points?apikey=%s",
        patronId, instanceId, apiKey))
      .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    JsonObject expected = new JsonObject(readMockFile(
      "/allowed_sp_mod_patron_expected_response.json"));
    JsonObject actual = new JsonObject(resp.body().asString());
    assertEquals(expected, actual);
  }
  @Test
  public void testAllowedServicePointsForItemError(TestContext context) throws Exception {
    logger.info("=== Test validation error during allowed service points request ===");

    final Response resp = RestAssured
      .with()
      .get(String.format("/patron/account/%s/item/%s/allowed-service-points?apikey=%s",
        patronId, itemId_notFound, apiKey))
      .then()
      .statusCode(422)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    JsonObject expected = new JsonObject(readMockFile(
      "/allowed_sp_error_edge_patron.json"));
    JsonObject actual = new JsonObject(resp.body().asString());
    assertEquals(expected, actual);
  }

  @Test
  public void testAllowedServicePointsForInstanceError(TestContext context) throws Exception {
    logger.info("=== Test validation error during allowed service points request ===");

    final Response resp = RestAssured
      .with()
      .get(String.format("/patron/account/%s/instance/%s/allowed-service-points?apikey=%s",
        patronId, instanceId_notFound, apiKey))
      .then()
      .statusCode(422)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    JsonObject expected = new JsonObject(readMockFile(
      "/allowed_sp_error_edge_patron.json"));
    JsonObject actual = new JsonObject(resp.body().asString());
    assertEquals(expected, actual);
  }

  @Test
  public void testCancelHoldSuccess(TestContext context) throws Exception {
    logger.info("=== Test cancel hold success ===");

    String cancedHoldJson = PatronMockOkapi.getHoldCancellation(holdCancellationHoldId, patronId);

    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(cancedHoldJson)
      .post(
        String.format("/patron/account/%s/hold/%s/cancel?apikey=%s", patronId, holdCancellationHoldId, apiKey))
      .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    Hold expected = Hold.fromJson(PatronMockOkapi.getRemovedHoldJson(holdCancellationHoldId));
    Hold actual = Hold.fromJson(resp.body().asString());

    assertEquals(expected, actual);
    assertEquals(holdCancellationHoldId, expected.requestId);
    assertEquals(PatronMockOkapi.holdCancellationReasonId, actual.cancellationReasonId);
    assertEquals(Hold.Status.CLOSED_CANCELED, actual.status);
    assertEquals(0, actual.queuePosition);
    assertEquals(expected.canceledByUserId, actual.canceledByUserId);
  }

  @Test
  public void testCancelHoldSuccessWithNonUUIDCanceledById(TestContext context) throws Exception {
    logger.info("=== Test cancel hold success ===");

    String cancedHoldJson = PatronMockOkapi.getHoldCancellation(
                                holdCancellationHoldId,
                                nonUUIDHoldCanceledByPatronId);

    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(cancedHoldJson)
      .post(
        String.format("/patron/account/%s/hold/%s/cancel?apikey=%s", patronId, holdCancellationHoldId, apiKey))
      .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    Hold expected = Hold.fromJson(PatronMockOkapi.getRemovedHoldJson(holdCancellationHoldId));
    Hold actual = Hold.fromJson(resp.body().asString());

    assertEquals(expected.requestId, actual.requestId);
    assertEquals(PatronMockOkapi.holdCancellationReasonId, actual.cancellationReasonId);
    assertEquals(Hold.Status.CLOSED_CANCELED, actual.status);
    assertEquals(0, actual.queuePosition);
    assertEquals(expected.canceledByUserId, actual.canceledByUserId);
  }

  @Test
  public void testCancelHoldPatronInvalidRequest(TestContext context) throws Exception {
    logger.info("=== Test cancel hold w/invalid cancellation request ===");
    int statusCode = 422;

    String cancedHoldJson = PatronMockOkapi.getInvalidHoldCancellation(invalidHoldCancellationdHoldId);
    String coreMessage = "required fields for cancelling holds are missing (holdId, cancellationReasonId)";

    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(cancedHoldJson)
      .post(
        String.format("/patron/account/%s/hold/%s/cancel?apikey=%s", patronId, invalidHoldCancellationdHoldId, apiKey))
      .then()
      .statusCode(statusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

    assertEquals(statusCode, msg.httpStatusCode);
    assertTrue(msg.message.contains(coreMessage));
  }

  @Test
  public void testCancelHoldPatronNotFound(TestContext context) throws Exception {
    logger.info("=== Test cancel hold w/ patron not found ===");
    int statusCode = 404;
    String cancedHoldJson = PatronMockOkapi.getHoldCancellation(holdCancellationHoldId, PatronMockOkapi.extPatronId_notFound);

    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(cancedHoldJson)
      .post(
          String.format("/patron/account/%s/hold/%s/cancel?apikey=%s", PatronMockOkapi.extPatronId_notFound, holdCancellationHoldId, apiKey))
      .then()
      .statusCode(statusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

      ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

      assertEquals(statusCode, msg.httpStatusCode);
      assertEquals("Unable to find patron " + PatronMockOkapi.extPatronId_notFound, msg.message);
  }

  @Test
  public void testCancelHoldHoldNotFound(TestContext context) throws Exception {
    logger.info("=== Test cancel hold w/ hold not found ===");

    int expectedStatusCode = 404;
    String expectedErrorMessage = "request record with ID \"" + PatronMockOkapi.holdReqId_notFound  + "\" cannot be found";
    String cancedHoldJson = PatronMockOkapi.getHoldCancellation(holdReqId_notFound,extPatronId);

    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(cancedHoldJson)
      .post(
          String.format("/patron/account/%s/hold/%s/cancel?apikey=%s", extPatronId,
              PatronMockOkapi.holdReqId_notFound,
              apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals(expectedErrorMessage, msg.message);
    assertEquals(expectedStatusCode,  msg.httpStatusCode);
  }

  @Test
  public void testCancelHoldUnknownApiKey(TestContext context) throws Exception {
    logger.info("=== Test cancel hold with unknown apiKey (tenant) ===");

    int expectedStatusCode = 401;
    String cancedHoldJson = PatronMockOkapi.getHoldCancellation(holdCancellationHoldId, extPatronId);

    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(cancedHoldJson)
      .post(
          String.format("/patron/account/%s/hold/%s/cancel?apikey=%s",
            extPatronId,
            holdCancellationHoldId,
            unknownTenantApiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

      ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

    assertEquals(MSG_ACCESS_DENIED, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testCancelHoldBadApiKey(TestContext context) throws Exception {
    logger.info("=== Test cancel hold with malformed apiKey ===");

    int expectedStatusCode = 401;
    String cancedHoldJson = PatronMockOkapi.getHoldCancellation(holdCancellationHoldId, extPatronId);

    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(cancedHoldJson)
      .post(String.format("/patron/account/%s/hold/%s/cancel?apikey=%s", extPatronId, holdCancellationHoldId, badApiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    try {
        ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

        assertEquals(MSG_ACCESS_DENIED, msg.message);
        assertEquals(expectedStatusCode,  msg.httpStatusCode);
    }
    catch(IOException ex){
        Assert.fail("Exception threw: " + ex.getMessage());
    }
  }

  @Test
  public void testCancelHoldRequestTimeout(TestContext context) throws Exception {
    logger.info("=== Test cancel hold request timeout ===");

    String cancedHoldJson = PatronMockOkapi.getHoldCancellation(holdCancellationHoldId, extPatronId);

    mockOkapi.setDelay(requestTimeoutMs * 3);
    RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(cancedHoldJson)
      .post(String.format("/patron/account/%s/hold/%s/cancel?apikey=%s", extPatronId, holdCancellationHoldId,
          apiKey))
      .then()
      .contentType(APPLICATION_JSON)
      .statusCode(408)
      .body("code", is(408))
      .body("errorMessage", is(MSG_REQUEST_TIMEOUT));
  }

  @Test
  public void testCancelHoldWithMalformedJsonRequest(TestContext context) throws Exception {
    logger.info("=== Test cancel hold with malformed JSON Request (from mod-circ) ===");

    String cancedHoldJson = PatronMockOkapi.getHoldCancellation(malformedHoldCancellationHoldId, patronId);

    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(cancedHoldJson)
      .post(
        String.format("/patron/account/%s/hold/%s/cancel?apikey=%s", patronId, malformedHoldCancellationHoldId, apiKey))
      .then()
      .statusCode(500)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    assertTrue(resp.body().asString().contains("Internal Server Error"));
  }

  @Test
  public void testEditHoldPatronNotFound(TestContext context) throws Exception {
    logger.info("=== Test edit hold w/ patron not found ===");

    int expectedStatusCode = 404;
    String canceledHold = PatronMockOkapi.getHoldCancellation(holdCancellationHoldId, PatronMockOkapi.extPatronId_notFound);

    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(canceledHold)
      .post(
          String.format("/patron/account/%s/hold/%s/cancel?apikey=%s", PatronMockOkapi.extPatronId_notFound,
              holdCancellationHoldId,
              apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals("Unable to find patron " + PatronMockOkapi.extPatronId_notFound, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testEditHoldNotFound(TestContext context) throws Exception {
    logger.info("=== Test edit hold w/hold not found ===");
    int expectedStatusCode = 404;
    String expectedErrorMsg = "request record with ID \"" + holdReqId_notFound + "\" cannot be found";
    String canceledHold = PatronMockOkapi.getHoldCancellation(holdReqId_notFound, extPatronId);

    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(canceledHold)
      .post(
          String.format("/patron/account/%s/hold/%s/cancel?apikey=%s", extPatronId, holdReqId_notFound,
              apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals(expectedErrorMsg, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testEditHoldUnknownApiKey(TestContext context) throws Exception {
    logger.info("=== Test edit hold with unknown apiKey (tenant) ===");

    int expectedStatusCode = 401;
    String canceledHold = PatronMockOkapi.getHoldCancellation(holdReqId_notFound, extPatronId);

    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(canceledHold)
      .post(
          String.format("/patron/account/%s/hold/%s/cancel?apikey=%s", extPatronId, holdCancellationHoldId,
              unknownTenantApiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals(MSG_ACCESS_DENIED, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testEditHoldBadApiKey(TestContext context) throws Exception {
    logger.info("=== Test edit hold with malformed apiKey ===");

    int expectedStatusCode = 401;
    String canceledHold = PatronMockOkapi.getHoldCancellation(holdReqId_notFound, extPatronId);

    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(canceledHold)
      .post(String.format("/patron/account/%s/hold/%s/cancel?apikey=%s", extPatronId, holdId, badApiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

    assertEquals(MSG_ACCESS_DENIED, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  public void testEditHoldRequestTimeout(TestContext context) throws Exception {
    logger.info("=== Test edit hold request timeout ===");

    String canceledHold = PatronMockOkapi.getHoldCancellation(holdReqId_notFound, extPatronId);

    mockOkapi.setDelay(requestTimeoutMs * 3);
    RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(canceledHold)
      .post(String.format("/patron/account/%s/hold/%s/cancel?apikey=%s", extPatronId, holdId, apiKey))
      .then()
      .contentType(APPLICATION_JSON)
      .statusCode(408)
      .body("code", is(408))
      .body("errorMessage", is(MSG_REQUEST_TIMEOUT));
  }

  @Test
  public void testCachedToken(TestContext context) throws Exception {
    logger.info("=== Test the tokens are cached and reused ===");

    Account expected = Account.fromJson(PatronMockOkapi.getAccountJson(patronId, false, false, false));
    int iters = 5;

    for (int i = 0; i < iters; i++) {
      final Response resp = RestAssured
        .get(String.format("/patron/account/%s?apikey=%s", extPatronId, apiKey))
        .then()
        .contentType(APPLICATION_JSON)
        .statusCode(200)
        .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .extract()
        .response();

      assertEquals(expected, Account.fromJson(resp.body().asString()));
    }

    verify(mockOkapi).loginHandler(any());
    verify(mockOkapi, atLeast(iters)).getAccountHandler(any());
  }

  private void validateHolds(Hold expectedHolds, Hold actualHolds) {
    assertEquals(expectedHolds.requestId, actualHolds.requestId);
    assertEquals(expectedHolds.pickupLocationId, actualHolds.pickupLocationId);
    assertEquals(expectedHolds.queuePosition, actualHolds.queuePosition);
    assertEquals(expectedHolds.status, actualHolds.status);
    assertEquals(expectedHolds.item, actualHolds.item);

    long expectedRequestDateTs = expectedHolds.requestDate != null
      //have to add 1 day's milliseconds because the Request timestamp is 1 day in the past
      ? expectedHolds.requestDate.getTime() + DAY_IN_MILLIS
      : Instant.now().toEpochMilli();

    long actualRequestDateTs = actualHolds.requestDate != null
      ? actualHolds.requestDate.getTime()
      : Instant.now().toEpochMilli();

    //check that the actual timestamp is within a minute of the expected timestamp
    assertTrue((actualRequestDateTs - expectedRequestDateTs)/1000 < 60);
  }

  private String swapExpirationDate(String jsonRequestMessage, String dateString) {
    return jsonRequestMessage.replaceFirst("(?<=\"expirationDate\").*\".*\"", dateString);
  }
}

