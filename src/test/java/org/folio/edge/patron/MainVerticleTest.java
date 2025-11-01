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
import static org.folio.edge.core.Constants.X_OKAPI_TENANT;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;
import static org.folio.edge.patron.Constants.KEYCLOAK_URL;
import static org.folio.edge.patron.Constants.MSG_ACCESS_DENIED;
import static org.folio.edge.patron.Constants.MSG_BATCH_REQUEST_NOBODY;
import static org.folio.edge.patron.Constants.MSG_HOLD_NOBODY;
import static org.folio.edge.patron.Constants.MSG_REQUEST_TIMEOUT;
import static org.folio.edge.patron.utils.PatronMockOkapi.batchRequestId;
import static org.folio.edge.patron.utils.PatronMockOkapi.batchRequestId_notFound;
import static org.folio.edge.patron.utils.PatronMockOkapi.extPatronId_notFound;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.restassured.RestAssured;
import io.restassured.config.DecoderConfig;
import io.restassured.config.DecoderConfig.ContentDecoder;
import io.restassured.response.Response;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.ApiKeyUtils;
import org.folio.edge.core.utils.test.TestUtils;
import org.folio.edge.patron.model.Account;
import org.folio.edge.patron.model.Hold;
import org.folio.edge.patron.model.Loan;
import org.folio.edge.patron.model.error.ErrorMessage;
import org.folio.edge.patron.utils.JwtTokenUtil;
import org.folio.edge.patron.utils.PatronMockOkapi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


@ExtendWith({VertxExtension.class})
class MainVerticleTest {

  private static final Logger logger = LogManager.getLogger(MainVerticleTest.class);
  private static final String extPatronId = PatronMockOkapi.extPatronId;
  private static final String patronId = PatronMockOkapi.patronId;
  private static final String itemId = UUID.randomUUID().toString();
  private static final String instanceId = UUID.randomUUID().toString();
  private static final String holdId = UUID.randomUUID().toString();
  private static final String EXTERNAL_SYSTEM_ID = UUID.randomUUID().toString();
  private static final String apiKey = ApiKeyUtils.generateApiKey(10, "diku", "diku");
  private static final String badApiKey = apiKey + "0000";
  private static final String unknownTenantApiKey = ApiKeyUtils.generateApiKey(10, "bogus", "diku");

  private static final long requestTimeoutMs = 3000L;

  private static Vertx vertx;
  private static PatronMockOkapi mockOkapi;
  private static JwtTokenUtil jwtTokenUtil;

  @BeforeAll
  static void setUpOnce(VertxTestContext context) throws Exception {
    int okapiPort = TestUtils.getPort();
    int serverPort = TestUtils.getPort();

    List<String> knownTenants = new ArrayList<>();
    knownTenants.add(ApiKeyUtils.parseApiKey(apiKey).tenantId);

    vertx = Vertx.vertx();
    jwtTokenUtil = new JwtTokenUtil();

    System.setProperty(SYS_PORT, String.valueOf(serverPort));
    System.setProperty(SYS_OKAPI_URL, "http://localhost:" + okapiPort);
    System.setProperty(SYS_SECURE_STORE_PROP_FILE, "src/main/resources/ephemeral.properties");
    System.setProperty(SYS_LOG_LEVEL, "DEBUG");
    System.setProperty(SYS_RESPONSE_COMPRESSION, "true");
    System.setProperty(SYS_REQUEST_TIMEOUT_MS, String.valueOf(requestTimeoutMs));
    System.setProperty(KEYCLOAK_URL, "http://localhost:" + okapiPort);

    final Checkpoint verticleStarted = context.checkpoint(1);
    mockOkapi = spy(new PatronMockOkapi(okapiPort, knownTenants));
    mockOkapi.start()
    .compose(x -> {
      final DeploymentOptions opt = new DeploymentOptions();
      return vertx.deployVerticle(MainVerticle.class.getName(), opt);
    })
    .onComplete(context.succeeding(id -> verticleStarted.flag()));

    RestAssured.baseURI = "http://localhost:" + serverPort;
    RestAssured.port = serverPort;
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @AfterAll
  static void tearDownOnce(VertxTestContext context) {
    logger.info("Shutting down server");
    mockOkapi.close()
    .compose(x -> vertx.close())
      .onComplete(ar -> {
        if (ar.succeeded()) {
          logger.info("Successfully shut down mock Okapi and edge-patron server");
          context.completeNow();
        } else {
          context.failNow(ar.cause());
        }
      });
  }

  @AfterEach
  public void after() {
    mockOkapi.setDelay(0);
  }

  @Test
  void testAdminHealth() {
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
  void testGetAccountUnknownApiKey() throws Exception {
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
  void testGetAccountBadApiKey() throws Exception {
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
  void testGetAccountPatronFound() {
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
  void testGetAccountPatronFoundGzip() {
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
  void testGetAccountPatronFoundDeflate() {
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
  void testGetAccountPatronNotFound() throws IOException {
    logger.info("=== Test request where patron isn't found ===");

    int expectedStatusCode = 404;

    final Response resp = RestAssured
      .get(String.format("/patron/account/%s?apikey=%s", extPatronId_notFound, apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

    assertEquals("Unable to find patron " + extPatronId_notFound, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  void testSecureGetAccount() {
    final String expected = PatronMockOkapi.getAccountJson(patronId, false, false, false);
    RestAssured
      .given()
      .header(X_OKAPI_TOKEN, jwtTokenUtil.generateToken(extPatronId, true))
      .header(X_OKAPI_TENANT, "diku")
      .get(String.format("/patron/account?apikey=%s", apiKey))
      .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .body(is(expected));
  }

  @Test
  void testSecureGetAccountInvalidToken() throws Exception {
    var resp = RestAssured
      .given()
      .header(X_OKAPI_TOKEN, jwtTokenUtil.generateToken(extPatronId, true) + "001")
      .header(X_OKAPI_TENANT, "diku")
      .get(String.format("/patron/account?apikey=%s", apiKey))
      .then()
      .statusCode(400)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var error = ErrorMessage.fromJson(resp.body().asString());
    assertEquals("Failed to validate access token", error.message);
    assertEquals(400, error.httpStatusCode);
  }

  @Test
  void testSecureGetAccountMissingTenant() throws Exception {
    var resp = RestAssured
      .given()
      .header(X_OKAPI_TOKEN, jwtTokenUtil.generateToken(extPatronId, true) + "001")
      .get(String.format("/patron/account?apikey=%s", apiKey))
      .then()
      .statusCode(400)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var error = ErrorMessage.fromJson(resp.body().asString());
    assertEquals("Missing tenant id", error.message);
    assertEquals(400, error.httpStatusCode);
  }

  @Test
  void testSecureGetAccountMissingToken() throws Exception {
    var resp = RestAssured
      .given()
      .header(X_OKAPI_TENANT, "diku")
      .get(String.format("/patron/account?apikey=%s", apiKey))
      .then()
      .statusCode(400)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var error = ErrorMessage.fromJson(resp.body().asString());
    assertEquals("Missing access token", error.message);
    assertEquals(400, error.httpStatusCode);
  }

  @Test
  void testSecureGetAccountMissingKeycloakPublicKey() throws Exception {
    var resp = RestAssured
      .given()
      .header(X_OKAPI_TENANT, "test")
      .header(X_OKAPI_TOKEN, jwtTokenUtil.generateToken(extPatronId, true))
      .get(String.format("/patron/account?apikey=%s", apiKey))
      .then()
      .statusCode(400)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var error = ErrorMessage.fromJson(resp.body().asString());
    assertEquals("Failed to validate access token", error.message);
    assertEquals(400, error.httpStatusCode);
  }

  @Test
  void testSecureGetAccountWithMissingClaims() throws Exception {
    var resp = RestAssured
      .given()
      .header(X_OKAPI_TENANT, "diku")
      .header(X_OKAPI_TOKEN, jwtTokenUtil.generateToken())
      .get(String.format("/patron/account?apikey=%s", apiKey))
      .then()
      .statusCode(400)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var error = ErrorMessage.fromJson(resp.body().asString());
    assertEquals("Token doesn't contain required claims", error.message);
    assertEquals(400, error.httpStatusCode);
  }

  @Test
  void testSecureGetAccountWithPatronNotVip() throws Exception {
    var resp = RestAssured
      .given()
      .header(X_OKAPI_TENANT, "diku")
      .header(X_OKAPI_TOKEN, jwtTokenUtil.generateToken(extPatronId, false))
      .get(String.format("/patron/account?apikey=%s", apiKey))
      .then()
      .statusCode(401)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var error = ErrorMessage.fromJson(resp.body().asString());
    assertEquals("Access Denied", error.message);
    assertEquals(401, error.httpStatusCode);
  }

  @Test
  void testGetPatronRegistrationStatusWithoutEmail() {

    var response = RestAssured
      .get(String.format("/patron/registration-status?apikey=%s", apiKey))
      .then()
      .statusCode(400)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var jsonResponse = new JsonObject(response.body().asString());
    assertEquals("INVALID_IDENTIFIERS", jsonResponse.getString("code"));
    assertEquals("Either emailId or externalSystemId must be provided in the request.", jsonResponse.getString("errorMessage"));

    response = RestAssured
      .get(String.format("/patron/registration-status?emailId=%s&apikey=%s", "", apiKey))
      .then()
      .statusCode(400)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    jsonResponse = new JsonObject(response.body().asString());
    assertEquals("INVALID_IDENTIFIERS", jsonResponse.getString("code"));
    assertEquals("Either emailId or externalSystemId must be provided in the request.", jsonResponse.getString("errorMessage"));
  }

  @Test
  void testGetPatronRegistrationStatusWithEmailAndESID() {

    var response = RestAssured
      .get(String.format("/patron/registration-status?emailId=%s&externalSystemId=%s&apikey=%s", "abc@abc.com", "9eb67301-6f6e-468f-9b1a-6134dc39a670", apiKey))
      .then()
      .statusCode(400)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var jsonResponse = new JsonObject(response.body().asString());
    assertEquals("INVALID_IDENTIFIERS", jsonResponse.getString("code"));
    assertEquals("Provide either emailId or externalSystemId, not both.", jsonResponse.getString("errorMessage"));

  }


  @Test
  void testGetPatronRegistrationStatusWithActiveEmail() {

    final var response = RestAssured
      .get(String.format("/patron/registration-status?emailId=%s&apikey=%s", "active@folio.com", apiKey))
      .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var expected = new JsonObject(readMockFile(
      "/user_active.json"));
    var actual = new JsonObject(response.body().asString());
    assertEquals(expected, actual);
  }

  @Test
  void testGetPatronRegistrationStatusWithExternalSystemId() {

    final var response = RestAssured
      .get(String.format("/patron/registration-status?externalSystemId=%s&apikey=%s", "9eb67301-6f6e-468f-9b1a-6134dc39a699", apiKey))
      .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var expected = new JsonObject(readMockFile(
      "/user_active.json"));
    var actual = new JsonObject(response.body().asString());
    assertEquals(expected, actual);
  }

  @Test
  void testGetPatronRegistrationStatusWithInvalidEmail() {

    final var response = RestAssured
      .get(String.format("/patron/registration-status?emailId=%s&apikey=%s", "usernotfound@folio.com", apiKey))
      .then()
      .statusCode(404)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var jsonResponse = new JsonObject(response.body().asString());
    assertEquals("USER_NOT_FOUND", jsonResponse.getString("code"));
    assertEquals("User does not exist", jsonResponse.getString("errorMessage"));
  }

  @Test
  void testGetPatronRegistrationStatusWithInvalidExternalSystemId() {

    final var response = RestAssured
      .get(String.format("/patron/registration-status?externalSystemId=%s&apikey=%s", "9eb67301-6f6e-468f-9b1a-6134dc39a700", apiKey))
      .then()
      .statusCode(404)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var jsonResponse = new JsonObject(response.body().asString());
    assertEquals("USER_NOT_FOUND", jsonResponse.getString("code"));
    assertEquals("User does not exist", jsonResponse.getString("errorMessage"));
  }


  @Test
  void testGetPatronRegistrationStatusWithMultipleUserEmail() {

    final var response = RestAssured
      .get(String.format("/patron/registration-status?emailId=%s&apikey=%s", "multipleuser@folio.com", apiKey))
      .then()
      .statusCode(400)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var jsonResponse = new JsonObject(response.body().asString());
    assertEquals("MULTIPLE_USER_WITH_EMAIL", jsonResponse.getString("code"));
    assertEquals("Multiple users found with the same email", jsonResponse.getString("errorMessage"));
  }

  @Test
  void testGetPatronRegistrationStatusWithInvalidScenarios() {

    // when we are getting 404, we converted it to Errors.class. But there are cases where we get text/plain errors.
    // In that case, code will return the error as it is.
    var response = RestAssured
      .get(String.format("/patron/registration-status?emailId=%s&apikey=%s", "invalid@folio.com", apiKey))
      .then()
      .statusCode(404)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var jsonResponse = new JsonObject(response.body().asString());
    assertEquals("404", jsonResponse.getString("code"));
    assertEquals("Resource not found", jsonResponse.getString("errorMessage"));

    response = RestAssured
      .get(String.format("/patron/registration-status?emailId=%s&apikey=%s", "empty@folio.com", apiKey))
      .then()
      .statusCode(500)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    jsonResponse = new JsonObject(response.body().asString());
    assertEquals("500", jsonResponse.getString("code"));
    assertEquals("unable to retrieve user details", jsonResponse.getString("errorMessage"));
  }

  @Test
  void testGetAccountNoApiKey() throws Exception {
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
  void testGetAccountPatronFoundIncludeLoans() {
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
  void testGetAccountPatronFoundIncludeCharges() {
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
  void testGetAccountPatronFoundIncludeHolds() {
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
  void testGetAccountPatronFoundIncludeAll() {
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
  void testGetAccountPatronFoundIncludeAllAndLimitEqualsToOne() {
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
  void testGetAccountPatronFoundIncludeLoansAndSortByAndNegativeLimit() throws Exception {
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
  void testGetAccountOffsetIsNegative() throws Exception {
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
  void testGetAccountOffsetIsNotNumber() throws Exception {
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
  void testGetAccountLimitIsNotNumber() throws Exception {
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
  void testGetAccountEmptyQueryArgs() {
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
  void testGetAccountRequestTimeout() {
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
  void testRenewSuccess() throws Exception {
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
  void testRenewPatronNotFound() throws Exception {
    logger.info("=== Test renew w/ patron not found ===");

    int expectedStatusCode = 404;

    final Response resp = RestAssured
      .post(
          String.format("/patron/account/%s/item/%s/renew?apikey=%s", extPatronId_notFound, itemId,
              apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals("Unable to find patron " + extPatronId_notFound, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  void testRenewItemNotFound() throws Exception {
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
  void testRenewUnknownApiKey() throws Exception {
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
  void testRenewBadApiKey() throws Exception {
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
  void testRenewRequestTimeout() {
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
  void testRenewRequesMaxRenewal() throws Exception {
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
  void testRenewRequesMaxRenewalWithEmptyErrors() throws Exception {
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
  void testRenewRequesMaxRenewalWithBadJson() throws Exception {
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
  void testPlaceInstanceHoldSuccess() throws Exception {
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
  void testSecurePlaceInstanceHoldSuccess() throws Exception {
    Hold hold = PatronMockOkapi.getHold(instanceId);

    final Response resp = RestAssured
      .with()
      .header(X_OKAPI_TOKEN, jwtTokenUtil.generateToken(patronId, true))
      .header(X_OKAPI_TENANT, "diku")
      .body(hold.toJson())
      .contentType(APPLICATION_JSON)
      .post(
        String.format("/patron/account/instance/%s/hold?apikey=%s", instanceId, apiKey))
      .then()
      .statusCode(201)
      .extract()
      .response();

    Hold expected = Hold.fromJson(PatronMockOkapi.getPlacedHoldJson(hold));
    Hold actual = Hold.fromJson(resp.body().asString());
    validateHolds(expected, actual);
  }

  @Test
  void testPlaceInstanceHoldPatronNotFound() throws Exception {
    logger.info("=== Test place instance hold w/ patron not found ===");

    Hold hold = PatronMockOkapi.getHold(instanceId);
    int expectedStatusCode = 404;

    final Response resp = RestAssured
      .with()
      .body(hold.toJson())
      .contentType(APPLICATION_JSON)
      .post(
          String.format("/patron/account/%s/instance/%s/hold?apikey=%s", extPatronId_notFound,
              instanceId,
              apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals("Unable to find patron " + extPatronId_notFound, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  void testPostPatron_201() {
    logger.info("=== testPostPatron_201 ===");
    JsonObject jsonObject = new JsonObject(readMockFile("/staging-users-post-request.json"));
    jsonObject.getJsonObject("generalInfo").put("firstName", "TEST_STATUS_CODE_201");
    RestAssured
      .with()
      .body(jsonObject.encode())
      .contentType(APPLICATION_JSON)
      .post(
        String.format("/patron?apikey=%s", apiKey))
      .then()
      .statusCode(201)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
  }

  @Test
  void testPutPatron_200() {
    logger.info("=== testPutPatron_200 ===");
    JsonObject jsonObject = new JsonObject(readMockFile("/staging-users-put-request.json"));
    jsonObject.getJsonObject("generalInfo").put("firstName", "TEST_STATUS_CODE_200");
    RestAssured
      .with()
      .body(jsonObject.encode())
      .contentType(APPLICATION_JSON)
      .put(
        String.format("/patron/%s?apikey=%s", EXTERNAL_SYSTEM_ID, apiKey))
      .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
  }


  @Test
  void testPostPatron_200() {
    logger.info("=== testPostPatron_200 ===");
    JsonObject jsonObject = new JsonObject(readMockFile("/staging-users-post-request.json"));
    jsonObject.getJsonObject("generalInfo").put("firstName", "TEST_STATUS_CODE_200");
    RestAssured
      .with()
      .body(jsonObject.encode())
      .contentType(APPLICATION_JSON)
      .post(
        String.format("/patron?apikey=%s", apiKey))
      .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
  }

  @Test
  void testPostPatron_400() {
    logger.info("=== testPostPatron_400 ===");
    JsonObject jsonObject = new JsonObject(readMockFile("/staging-users-post-request.json"));
    jsonObject.getJsonObject("generalInfo").put("firstName", "TEST_STATUS_CODE_400");
    RestAssured
      .with()
      .body(jsonObject.encode())
      .contentType(APPLICATION_JSON)
      .post(
        String.format("/patron?apikey=%s", apiKey))
      .then()
      .statusCode(400)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .body("errorMessage", is("A bad exception occurred"))
      .body("code", is(400));
  }

  @Test
  void testPutPatron_400() {
    logger.info("=== testPutPatron_400 ===");
    JsonObject jsonObject = new JsonObject(readMockFile("/staging-users-put-request.json"));
    jsonObject.getJsonObject("generalInfo").put("firstName", "TEST_STATUS_CODE_400");
    RestAssured
      .with()
      .body(jsonObject.encode())
      .contentType(APPLICATION_JSON)
      .put(
        String.format("/patron/%s?apikey=%s", EXTERNAL_SYSTEM_ID, apiKey))
      .then()
      .statusCode(400)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .body("errorMessage", is("A bad exception occurred"))
      .body("code", is(400));
  }


  @Test
  void testPostPatron_422() {
    logger.info("=== testPostPatron_422 ===");
    JsonObject jsonObject = new JsonObject(readMockFile("/staging-users-post-request.json"));
    jsonObject.getJsonObject("generalInfo").put("firstName", "TEST_STATUS_CODE_422");
    RestAssured
      .with()
      .body(jsonObject.encode())
      .contentType(APPLICATION_JSON)
      .post(
        String.format("/patron?apikey=%s", apiKey))
      .then()
      .statusCode(422)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .body("errorMessage", is("ABC is required"))
      .body("code", is(422));
  }

  @Test
  void testPutPatron_422() {
    logger.info("=== testPutPatron_422 ===");
    JsonObject jsonObject = new JsonObject(readMockFile("/staging-users-put-request.json"));
    jsonObject.getJsonObject("generalInfo").put("firstName", "TEST_STATUS_CODE_422");
    RestAssured
      .with()
      .body(jsonObject.encode())
      .contentType(APPLICATION_JSON)
      .put(
        String.format("/patron/%s?apikey=%s", EXTERNAL_SYSTEM_ID, apiKey))
      .then()
      .statusCode(422)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .body("errorMessage", is("ABC is required"))
      .body("code", is(422));
  }

  @Test
  void testPostPatron_500() {
    logger.info("=== testPostPatron_500 ===");
    JsonObject jsonObject = new JsonObject(readMockFile("/staging-users-post-request.json"));
    jsonObject.getJsonObject("generalInfo").put("firstName", "TEST_STATUS_CODE_500");
    RestAssured
      .with()
      .body(jsonObject.encode())
      .contentType(APPLICATION_JSON)
      .post(
        String.format("/patron?apikey=%s", apiKey))
      .then()
      .statusCode(500)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .body("errorMessage", is("Server exception occurred"))
      .body("code", is(500));
  }

  @Test
  void testPostPatron_NoRequestBody() {
    logger.info("=== testPostPatron_NoRequestBody ===");
    RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .post(
        String.format("/patron?apikey=%s", apiKey))
      .then()
      .statusCode(400)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .body("errorMessage", is("Request body must not be null"))
      .body("code", is("MISSING_BODY"));
  }

  @Test
  void testPutPatron_500() {
    logger.info("=== testPutPatron_500 ===");
    JsonObject jsonObject = new JsonObject(readMockFile("/staging-users-put-request.json"));
    jsonObject.getJsonObject("generalInfo").put("firstName", "TEST_STATUS_CODE_500");
    RestAssured
      .with()
      .body(jsonObject.encode())
      .contentType(APPLICATION_JSON)
      .put(
        String.format("/patron/%s?apikey=%s", EXTERNAL_SYSTEM_ID, apiKey))
      .then()
      .statusCode(500)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .body("errorMessage", is("Server exception occurred"))
      .body("code", is(500));
  }

  @Test
  void testPutPatron_NoRequestBody() {
    logger.info("=== testPutPatron_NoRequestBody ===");
    RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .put(
        String.format("/patron/%s?apikey=%s", EXTERNAL_SYSTEM_ID, apiKey))
      .then()
      .statusCode(400)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .body("errorMessage", is("Request body must not be null"))
      .body("code", is("MISSING_BODY"));
  }

  @Test
  void testPutPatron_NoParam() {
    logger.info("=== testPutPatron_NoParam ===");
    JsonObject jsonObject = new JsonObject(readMockFile("/staging-users-put-request.json"));
    jsonObject.getJsonObject("generalInfo").put("firstName", "TEST_STATUS_CODE_405");
    RestAssured
      .with()
      .body(jsonObject.encode())
      .contentType(APPLICATION_JSON)
      .put(
        String.format("/patron/%s?apikey=%s", "", apiKey))
      .then()
      .statusCode(405);
  }

  @Test
  void testPlaceInstanceHoldInstanceNotFound() throws Exception {
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
  void testPlaceInstanceHoldUnknownApiKey() throws Exception {
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
  void testPlaceInstanceHoldBadApiKey() throws Exception {
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
  void testPlaceInstanceHoldRequestTimeout() throws Exception {
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
  void testPlaceInstanceHoldNoBody() {
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
  void testPlaceItemHoldSuccess() throws Exception {
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
  void testPlaceItemHoldWithoutRequestDateSuccess() throws Exception {
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
  void testPlaceItemHoldWithInvalidExpirationDateSuccess() throws Exception {
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
  void testPlaceItemHoldWithValidExpirationDateSuccess() throws Exception {
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
  void testPlaceItemHoldPatronNotFound() throws Exception {
    logger.info("=== Test place item hold w/ patron not found ===");

    Hold hold = PatronMockOkapi.getHold(itemId);
    int expectedStatusCode = 404;

    final Response resp = RestAssured
      .with()
      .body(hold.toJson())
      .contentType(APPLICATION_JSON)
      .post(
          String.format("/patron/account/%s/item/%s/hold?apikey=%s", extPatronId_notFound, itemId,
              apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals("Unable to find patron " + extPatronId_notFound, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  void testPlaceItemHoldItemNotFound() throws Exception {
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
  void testPlaceItemHoldUnknownApiKey() throws Exception {
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
  void testPlaceItemHoldBadApiKey() throws Exception {
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
  void testPlaceItemHoldRequestTimeout() throws Exception {
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
  void testPlaceItemHoldRequestNoBody() {
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
  void testAllowedServicePointsSuccess() {
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
  void testSecureAllowedServicePointsSuccess() {

    final Response resp = RestAssured
      .with()
      .header(X_OKAPI_TOKEN, jwtTokenUtil.generateToken(patronId, true))
      .header(X_OKAPI_TENANT, "diku")
      .get(String.format("/patron/account/instance/%s/allowed-service-points?apikey=%s",
        instanceId, apiKey))
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
  void testAllowedServicePointsForItemError() {
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
  void testAllowedServicePointsForInstanceError() {
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
  void testMultiItemAllowedServicePointsForInstanceError() {
    logger.info("=== Test validation error during multi-item allowed service points request ===");

    var request = new JsonObject(readMockFile("/allowed_service_points_for_items_request.json")).encodePrettily();

    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(request)
      .post(String.format("/patron/account/%s/instance/%s/allowed-service-points-multi-item?apikey=%s",
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
  void testMultiItemAllowedServicePointsForInstanceSuccess() {
    logger.info("=== Test multi-item allowed service points request ===");

    var request = new JsonObject(readMockFile("/allowed_service_points_for_items_request.json")).encodePrettily();
    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(request)
      .post(String.format("/patron/account/%s/instance/%s/allowed-service-points-multi-item?apikey=%s",
        patronId, instanceId, apiKey))
      .then()
      .statusCode(200)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var expected = new JsonObject(readMockFile(
      "/multi_item_allowed_sp_mod_patron_expected_response.json"));
    JsonObject actual = new JsonObject(resp.body().asString());
    assertEquals(expected, actual);
  }

  @Test
  void testPostMultiItemBatchRequestPatronNotFoundError() throws IOException {
    logger.info("=== Test patron not found error during post multi-item batch request ===");

    var request = new JsonObject(readMockFile("/batch_request.json")).encodePrettily();

    var expectedStatusCode = 404;
    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(request)
      .post(String.format("/patron/account/%s/instance/%s/batch-request?apikey=%s",
        extPatronId_notFound, instanceId, apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals("Unable to find patron " + extPatronId_notFound, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  void testPostMultiItemBatchRequestInstanceNotFoundError() throws IOException {
    logger.info("=== Test instance not found error during post multi-item batch request ===");

    var request = new JsonObject(readMockFile("/batch_request.json")).encodePrettily();

    var expectedStatusCode = 404;
    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(request)
      .post(String.format("/patron/account/%s/instance/%s/batch-request?apikey=%s",
        patronId, instanceId_notFound, apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals("Instance '" + instanceId_notFound + "' isn't found", msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  void testPostMultiItemBatchRequestInvalidBodyError() throws IOException {
    logger.info("=== Test invalid body error during post multi-item batch request ===");

    var expectedStatusCode = 400;
    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .post(String.format("/patron/account/%s/instance/%s/batch-request?apikey=%s",
        patronId, instanceId, apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals(MSG_BATCH_REQUEST_NOBODY, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  void testPostMultiItemBatchRequestSuccess() {
    logger.info("=== Test post multi-item batch request ===");

    var request = new JsonObject(readMockFile("/batch_request.json")).encodePrettily();

    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(request)
      .post(String.format("/patron/account/%s/instance/%s/batch-request?apikey=%s",
        patronId, instanceId, apiKey))
      .then()
      .statusCode(201)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var expected = new JsonObject(readMockFile("/batch_request_expected_response.json"));
    var actual = new JsonObject(resp.body().asString());
    assertEquals(expected, actual);
  }

  @ParameterizedTest
  @MethodSource("batchRequestStatusNotFoundErrorParams")
  void testGetMultiItemBatchRequestStatusBatchRequestNotFoundError(String patronId, String instanceId, String batchId,
                                                                   String expectedMessage) throws IOException {
    logger.info("=== Test not found error during get multi-item batch request status ===");

    var expectedStatusCode = 404;
    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .get(String.format("/patron/account/%s/instance/%s/batch-request/%s/status?apikey=%s",
        patronId, instanceId, batchId, apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    var msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals(expectedMessage, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  static Stream<Arguments> batchRequestStatusNotFoundErrorParams() {
    return Stream.of(
      Arguments.of(extPatronId_notFound, instanceId, batchRequestId, "Unable to find patron " + extPatronId_notFound),
      Arguments.of(patronId, instanceId_notFound, batchRequestId, "Instance '" + instanceId_notFound + "' isn't found"),
      Arguments.of(patronId, instanceId, batchRequestId_notFound, "Multi-Item Batch request '" + batchRequestId_notFound + "' isn't found"));
  }

  @Test
  void testCancelHoldSuccess() throws Exception {
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
  void testSecureCancelHoldSuccess() throws Exception {
    String cancedHoldJson = PatronMockOkapi.getHoldCancellation(holdCancellationHoldId, patronId);

    final Response resp = RestAssured
      .with()
      .header(X_OKAPI_TOKEN, jwtTokenUtil.generateToken(patronId, true))
      .header(X_OKAPI_TENANT, "diku")
      .contentType(APPLICATION_JSON)
      .body(cancedHoldJson)
      .post(
        String.format("/patron/account/hold/%s/cancel?apikey=%s", holdCancellationHoldId, apiKey))
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
  void testCancelHoldSuccessWithNonUUIDCanceledById() throws Exception {
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
  void testCancelHoldPatronInvalidRequest() throws Exception {
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
  void testCancelHoldPatronNotFound() throws Exception {
    logger.info("=== Test cancel hold w/ patron not found ===");
    int statusCode = 404;
    String cancedHoldJson = PatronMockOkapi.getHoldCancellation(holdCancellationHoldId, extPatronId_notFound);

    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(cancedHoldJson)
      .post(
          String.format("/patron/account/%s/hold/%s/cancel?apikey=%s", extPatronId_notFound, holdCancellationHoldId, apiKey))
      .then()
      .statusCode(statusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

      ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());

      assertEquals(statusCode, msg.httpStatusCode);
      assertEquals("Unable to find patron " + extPatronId_notFound, msg.message);
  }

  @Test
  void testCancelHoldHoldNotFound() throws Exception {
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
  void testCancelHoldUnknownApiKey() throws Exception {
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
  void testCancelHoldBadApiKey() {
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
        fail("Exception threw: " + ex.getMessage());
    }
  }

  @Test
  void testCancelHoldRequestTimeout() {
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
  void testCancelHoldWithMalformedJsonRequest() {
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
  void testEditHoldPatronNotFound() throws Exception {
    logger.info("=== Test edit hold w/ patron not found ===");

    int expectedStatusCode = 404;
    String canceledHold = PatronMockOkapi.getHoldCancellation(holdCancellationHoldId, extPatronId_notFound);

    final Response resp = RestAssured
      .with()
      .contentType(APPLICATION_JSON)
      .body(canceledHold)
      .post(
          String.format("/patron/account/%s/hold/%s/cancel?apikey=%s", extPatronId_notFound,
              holdCancellationHoldId,
              apiKey))
      .then()
      .statusCode(expectedStatusCode)
      .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .extract()
      .response();

    ErrorMessage msg = ErrorMessage.fromJson(resp.body().asString());
    assertEquals("Unable to find patron " + extPatronId_notFound, msg.message);
    assertEquals(expectedStatusCode, msg.httpStatusCode);
  }

  @Test
  void testEditHoldNotFound() throws Exception {
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
  void testEditHoldUnknownApiKey() throws Exception {
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
  void testEditHoldBadApiKey() throws Exception {
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
  void testEditHoldRequestTimeout() {
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
  void testCachedToken() throws Exception {
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

