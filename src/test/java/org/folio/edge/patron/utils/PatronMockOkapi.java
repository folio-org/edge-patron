package org.folio.edge.patron.utils;

import static java.util.Collections.singletonList;
import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.DAY_IN_MILLIS;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;
import static org.folio.edge.patron.Constants.PARAM_HOLD_ID;
import static org.folio.edge.patron.Constants.PARAM_INCLUDE_CHARGES;
import static org.folio.edge.patron.Constants.PARAM_INCLUDE_HOLDS;
import static org.folio.edge.patron.Constants.PARAM_INCLUDE_LOANS;
import static org.folio.edge.patron.Constants.PARAM_INSTANCE_ID;
import static org.folio.edge.patron.Constants.PARAM_ITEM_ID;
import static org.folio.edge.patron.Constants.PARAM_LIMIT;
import static org.folio.edge.patron.Constants.PARAM_OFFSET;
import static org.folio.edge.patron.Constants.PARAM_PATRON_ID;
import static org.folio.edge.patron.Constants.PARAM_REQUEST_ID;
import static org.folio.edge.patron.Constants.PARAM_SORT_BY;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.test.MockOkapi;
import org.folio.edge.patron.model.*;
import org.folio.edge.patron.model.Hold.Status;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class PatronMockOkapi extends MockOkapi {

  private static final Logger logger = LogManager.getLogger(PatronMockOkapi.class);

  public static final String PARAM_QUERY = "query";

  public static final String isbn = "0008675309";
  public static final String instanceId = UUID.randomUUID().toString();
  public static final String instanceId_notFound = UUID.randomUUID().toString();
  public static final String holdReqId = UUID.randomUUID().toString();
  public static final String holdReqId_notFound = UUID.randomUUID().toString();
  public static final String pickupLocationId = UUID.randomUUID().toString();
  public static final String itemId_overdue = UUID.randomUUID().toString();
  public static final String itemId = UUID.randomUUID().toString();
  public static final String itemId_notFound = UUID.randomUUID().toString();
  public static final String patronId = UUID.randomUUID().toString();
  public static final String patronId_notFound = UUID.randomUUID().toString();
  public static final String extPatronId = UUID.randomUUID().toString();
  public static final String extPatronId_notFound = UUID.randomUUID().toString();
  public static final String feeFineId = UUID.randomUUID().toString();
  public static final String itemId_reached_max_renewals = UUID.randomUUID().toString();
  public static final String itemId_reached_max_renewals_empty_error_msg = UUID.randomUUID().toString();
  public static final String itemId_reached_max_renewals_bad_json_msg = UUID.randomUUID().toString();
  public static final String holdCancellationReasonId = UUID.randomUUID().toString();
  public static final String holdCancellationHoldId = "6b6b715e-8038-49ba-ab91-faa8fdf7449c";
  public static final String invalidHoldCancellationdHoldId = UUID.randomUUID().toString();
  public static final String malformedHoldCancellationHoldId = "6b6b715e-8038-49ba-ab91-faa8fdf7448d";
  public static final String goodRequestId = holdCancellationHoldId ;
  public static final String nonUUIDHoldCanceledByPatronId = "patron@folio.org";
  public static final String patronComments = "Can you deliver this to the History building for Professor Grant?";
  public static final String wrongIntegerParamMessage = "'%s' parameter is incorrect. parameter value {%s} is not valid: must be an integer, greater than or equal to 0";
  public static final String offset_param = "offset";
  public static final String limit_param = "limit";

  public static final long checkedOutTs = System.currentTimeMillis() - (34 * DAY_IN_MILLIS);
  public static final long dueDateTs = checkedOutTs + (20 * DAY_IN_MILLIS);
  public static final long accrualDateTs = checkedOutTs + (7 * DAY_IN_MILLIS);

  public static final long holdExpTs = System.currentTimeMillis() + (59 * DAY_IN_MILLIS);
  public static final long holdReqTs = System.currentTimeMillis() - DAY_IN_MILLIS;

  public static final String Sample422ErrorMsg = "{" +
          "\"errors\" : [ {" +
          "\"message\" : \"loan has reached its maximum number of renewals\"," +
          "\"parameters\" : [ {" +
          "\"key\" : \"loanPolicyName\"," +
          "\"value\" : \"Example Loan Policy\"" +
          "}, {" +
          "\"key\" : \"loanPolicyId\"," +
          "\"value\" : \"d9cd0bed-1b49-4b5e-a7bd-064b8d177231\"" +
          "} ]" +
          "} ]" +
          "}";

  public static final String Sample422BadJsonErrorMsg = "{" +
          "\"errors\" : [ {" +
          "\"message\" : \"loan has reached its maximum number of renewals\"," +
          "\"parameters\" : [ " +
          "\"key\" : \"loanPolicyName\"," +
          "\"value\" : \"Example Loan Policy\"" +
          "}, {" +
          "\"key\" : \"loanPolicyId\"," +
          "\"value\" : \"d9cd0bed-1b49-4b5e-a7bd-064b8d177231\"" +
          "} ]" +
          "} ]" +
          "}";

  public static final String SampleEmpty422ErrorMsg = "{" +
          "\"errors\" : []" +
          "}";

  public PatronMockOkapi(int port, List<String> knownTenants) {
    super(port, knownTenants);
  }

  @Override
  public Router defineRoutes() {
    Router router = super.defineRoutes();

    router.route(HttpMethod.GET, "/users")
      .handler(this::getPatronHandler);

    router.route(HttpMethod.GET, "/patron/account/:patronId")
      .handler(this::getAccountHandler);

    router.route(HttpMethod.POST, "/patron/account/:patronId/item/:itemId/renew")
      .handler(this::renewItemHandler);

    router.route(HttpMethod.POST, "/patron/account/:patronId/item/:itemId/hold")
      .handler(this::placeItemHoldHandler);

    router.route(HttpMethod.POST, "/patron/account/:patronId/instance/:instanceId/hold")
      .handler(this::placeInstanceHoldHandler);

    router.route(HttpMethod.POST, "/patron/account/:patronId/hold/:holdId/cancel")
      .handler(this::cancelHoldHandler);

    router.route(HttpMethod.GET, "/circulation/requests/:requestId")
      .handler(this::getRequestHandler);

    return router;
  }

  public void getPatronHandler(RoutingContext ctx) {
    String query = ctx.request().getParam(PARAM_QUERY);
    String token = ctx.request().getHeader(X_OKAPI_TOKEN);

    if (token == null || !token.equals(MOCK_TOKEN)) {
      ctx.response()
        .setStatusCode(403)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end("Access requires permission: users.collection.get");
    } else {
      String[] parts = query.split("==");
      String extPatronId = parts[1];

      ctx.response()
        .setStatusCode(200)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(getPatronJson(extPatronId));
    }
  }

  public void getAccountHandler(RoutingContext ctx) {
    String patronId = ctx.request().getParam(PARAM_PATRON_ID);
    String token = ctx.request().getHeader(X_OKAPI_TOKEN);
    boolean includeLoans = Boolean.parseBoolean(ctx.request().getParam(PARAM_INCLUDE_LOANS));
    boolean includeCharges = Boolean.parseBoolean(ctx.request().getParam(PARAM_INCLUDE_CHARGES));
    boolean includeHolds = Boolean.parseBoolean(ctx.request().getParam(PARAM_INCLUDE_HOLDS));
    String limit  = ctx.request().getParam(PARAM_LIMIT);
    String offset  = ctx.request().getParam(PARAM_OFFSET);
    String sortBy = ctx.request().getParam(PARAM_SORT_BY);

    if (token == null || !token.equals(MOCK_TOKEN)) {
      ctx.response()
        .setStatusCode(403)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end("Access requires permission: patron.account.get");
    } else if (patronId.equals(patronId_notFound)) {
      // Magic patronId signifying we want to mock a "not found"
      // response.
      ctx.response()
        .setStatusCode(404)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end(patronId + " not found");
    } else if ("-1".equals(offset)) {
      ctx.response()
        .setStatusCode(400)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end(String.format(wrongIntegerParamMessage, offset_param, "-1"));
    } else if ("-1".equals(limit)) {
      ctx.response()
        .setStatusCode(400)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end(String.format(wrongIntegerParamMessage, limit_param, "-1"));
    } else if ("1".equals(limit)) {
      ctx.response()
        .setStatusCode(200)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(getAccountWithSingleItemsJson(patronId, includeLoans, includeCharges, includeHolds));
    } else if (StringUtils.isNotEmpty(sortBy)) {
      ctx.response()
        .setStatusCode(200)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(getAccountWithSortedLoans(patronId));
    } else {
      ctx.response()
        .setStatusCode(200)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(getAccountJson(patronId, includeLoans, includeCharges, includeHolds));
    }
  }

  public void renewItemHandler(RoutingContext ctx) {
    String patronId = ctx.request().getParam(PARAM_PATRON_ID);
    String itemId = ctx.request().getParam(PARAM_ITEM_ID);
    String token = ctx.request().getHeader(X_OKAPI_TOKEN);

    if (token == null || !token.equals(MOCK_TOKEN)) {
      ctx.response()
        .setStatusCode(403)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end("Access requires permission: patron.loan.renew.post");
    } else if (patronId.equals(patronId_notFound)) {
      // Magic patronId signifying we want to mock a "not found"
      // response.
      ctx.response()
        .setStatusCode(404)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end(patronId + " not found");
    } else if (itemId.equals(itemId_notFound)) {
      // Magic patronId signifying we want to mock a "not found"
      // response.
      ctx.response()
        .setStatusCode(404)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end(itemId + " not found");
    } else if (itemId.equals(itemId_reached_max_renewals)) {
      ctx.response()
        .setStatusCode(422)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(Sample422ErrorMsg);
    } else if (itemId.equals(itemId_reached_max_renewals_empty_error_msg)) {
      ctx.response()
        .setStatusCode(422)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(SampleEmpty422ErrorMsg);
    } else if (itemId.equals(itemId_reached_max_renewals_bad_json_msg)) {
      ctx.response()
        .setStatusCode(422)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(Sample422BadJsonErrorMsg);
    } else {
      ctx.response()
        .setStatusCode(201)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(getLoanJson(patronId, itemId));
    }
  }

  public void placeItemHoldHandler(RoutingContext ctx) {
    String patronId = ctx.request().getParam(PARAM_PATRON_ID);
    String itemId = ctx.request().getParam(PARAM_ITEM_ID);
    String token = ctx.request().getHeader(X_OKAPI_TOKEN);

    String body = ctx.getBodyAsString();

    Hold hold;
    try {
      hold = Hold.fromJson(body);
    } catch (IOException e) {
      logger.error("Exception parsing request payload", e);
      ctx.response()
        .setStatusCode(400)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end("Bad Request");
      return;
    }

    if (token == null || !token.equals(MOCK_TOKEN)) {
      ctx.response()
        .setStatusCode(403)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end("Access requires permission: patron.item.hold.post");
    } else if (patronId.equals(patronId_notFound)) {
      // Magic patronId signifying we want to mock a "not found"
      // response.
      ctx.response()
        .setStatusCode(404)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end(patronId + " not found");
    } else if (itemId.equals(itemId_notFound)) {
      // Magic itemId signifying we want to mock a "not found"
      // response.
      ctx.response()
        .setStatusCode(404)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end(itemId + " not found");
    } else if (hold == null) {
      ctx.response()
        .setStatusCode(400)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end("Bad Request");
    } else {
      ctx.response()
        .setStatusCode(201)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(getPlacedHoldJson(hold));
    }
  }

  public void getRequestHandler(RoutingContext ctx) {
    String requestId = ctx.request().getParam(PARAM_REQUEST_ID);
    String token = ctx.request().getHeader(X_OKAPI_TOKEN);

    if (token == null || !token.equals(MOCK_TOKEN)) {
      ctx.response()
        .setStatusCode(403)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end("Access requires permission: users.collection.get");
    } else if (requestId.equals(holdReqId_notFound)) {
      ctx.response()
        .setStatusCode(404)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end("request record with ID \"" + requestId + "\" cannot be found");
    } else if (requestId.equals(malformedHoldCancellationHoldId)) {
      ctx.response()
        .setStatusCode(200)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(getRequest(requestId));
    } else {
      ctx.response()
        .setStatusCode(200)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(getRequest(requestId));
    }
  }

  public void cancelHoldHandler(RoutingContext ctx) {
    String patronId = ctx.request().getParam(PARAM_PATRON_ID);
    String holdId = ctx.request().getParam(PARAM_HOLD_ID);
    String token = ctx.request().getHeader(X_OKAPI_TOKEN);
    String body = ctx.getBodyAsString();

    if (token == null || !token.equals(MOCK_TOKEN)) {
      ctx.response()
        .setStatusCode(403)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end("Access requires permission: patron.hold.cancel.item.post");
    } else if (patronId.equals(patronId_notFound)) {
      // Magic patronId signifying we want to mock a "not found"
      // response.
      ctx.response()
        .setStatusCode(404)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end(patronId + " not found");
    } else if (holdId.equals(holdReqId_notFound)) {
      // Magic holdId signifying we want to mock a "not found"
      // response.
      ctx.response()
        .setStatusCode(404)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end("request record with ID \"" + holdId + "\" cannot be found");
    } else {
      ctx.response()
        .setStatusCode(200)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(getRemovedHoldJson(holdId));
    }
  }

  public void placeInstanceHoldHandler(RoutingContext ctx) {
    String patronId = ctx.request().getParam(PARAM_PATRON_ID);
    String instanceId = ctx.request().getParam(PARAM_INSTANCE_ID);
    String token = ctx.request().getHeader(X_OKAPI_TOKEN);

    String body = ctx.getBodyAsString();

    Hold hold;
    try {
      hold = Hold.fromJson(body);
    } catch (IOException e) {
      logger.error("Exception parsing request payload", e);
      ctx.response()
          .setStatusCode(400)
          .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
          .end("Bad Request");
      return;
    }

    if (token == null || !token.equals(MOCK_TOKEN)) {
      ctx.response()
          .setStatusCode(403)
          .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
          .end("Access requires permission: patron.instance.hold.post");
    } else if (patronId.equals(patronId_notFound)) {
      // Magic patronId signifying we want to mock a "not found"
      // response.
      ctx.response()
          .setStatusCode(404)
          .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
          .end(String.format("Patron '%s' isn't found", patronId));
    } else if (instanceId.equals(instanceId_notFound)) {
      // Magic itemId signifying we want to mock a "not found"
      // response.
      ctx.response()
          .setStatusCode(404)
          .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
          .end(String.format("Instance '%s' isn't found", instanceId));
    } else if (hold == null) {
      ctx.response()
          .setStatusCode(400)
          .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
          .end("Bad Request");
    } else {
      ctx.response()
          .setStatusCode(201)
          .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
          .end(getPlacedHoldJson(hold));
    }
  }

  public static String getPatronJson(String extPatronId) {
    JsonArray users = new JsonArray();
    logger.info(extPatronId_notFound);
    logger.info(extPatronId);
    if (!extPatronId_notFound.equals(extPatronId)) {
      users.add(new JsonObject()
        .put("externalSystemId", extPatronId)
        .put("id", patronId));
    }

    JsonObject json = new JsonObject()
      .put("users", users)
      .put("totalRecords", users.size())
      .put("resultInfo", new JsonObject()
        .put("totalRecords", users.size())
        .put("facets", new JsonArray())
        .put("diagnostics", new JsonArray()));

    return json.encodePrettily();
  }

  public static String getRequest(String requestId) {
    String request = null;
    if (requestId.equals(goodRequestId)) {
      request = readMockFile("/good-request.json");
    } else if (requestId.equals(malformedHoldCancellationHoldId)) {
      request = readMockFile("/malformed-request.json");
    }
    return request;
  }

  public static String getAccountJson(String patronId, boolean includeLoans, boolean includeCharges,
      boolean includeHolds) {

    Account.Builder acctBldr = Account.builder()
      .id(patronId);

    List<Charge> charges = new ArrayList<>();
    charges.add(getCharge(itemId_overdue));
    charges.add(getCharge(itemId));
    acctBldr.charges(charges);

    List<Hold> holds = new ArrayList<>();
    holds.add(getHold(itemId_overdue));
    holds.add(getHold(itemId));
    acctBldr.holds(holds);

    List<Loan> loans = new ArrayList<>();
    loans.add(getLoan(itemId_overdue));
    loans.add(getLoan(itemId));
    acctBldr.loans(loans);

    return builderToJson(acctBldr, includeLoans, includeCharges, includeHolds);
  }

  public static String getPatronInfoJson(String patronId) {
    Patron.Builder ptrnbldr = Patron.builder().id(patronId);
    //TODO
    return builderToJson(ptrnbldr);
  }

  public static String getAccountWithSingleItemsJson(String patronId, boolean includeLoans, boolean includeCharges,
    boolean includeHolds) {

    Account.Builder acctBldr = Account.builder()
      .id(patronId);
    acctBldr.loans(singletonList(getLoan(itemId)));
    acctBldr.holds(singletonList(getHold(itemId)));
    acctBldr.charges(singletonList(getCharge(itemId)));

    return builderToJson(acctBldr, includeLoans, includeCharges, includeHolds);
  }

  public static String getAccountWithSortedLoans(String patronId) {

    Account.Builder acctBldr = Account.builder()
      .id(patronId);
    List<Loan> loans = new ArrayList<>();
    loans.add(getLoan(itemId));
    loans.add(getLoan(itemId_overdue));
    acctBldr.loans(loans);

    return builderToJson(acctBldr, true, false, false);
  }

  private static String builderToJson(Account.Builder acctBldr, boolean includeLoans, boolean includeCharges,
    boolean includeHolds) {
    String ret = null;
    try {
      ret = acctBldr.build().toJson(includeLoans, includeCharges, includeHolds);
    } catch (JsonProcessingException e) {
      logger.warn("Failed to generate Account JSON", e);
    }
    return ret;
  }

  private static String builderToJson(Patron.Builder ptrnBldr) {
    String ret = null;
    try {
      ret = ptrnBldr.build().toJson();
    } catch (JsonProcessingException e) {
      logger.warn("Failed to generate Patron JSON", e);
    }
    return ret;
  }

  public static Item getItem(String itemId) {
    return Item.builder()
      .itemId(itemId)
      .title("The Stars My Destination")
      .author("Bester, Alfred")
      .instanceId(instanceId)
      .isbn(isbn)
      .build();
  }

  public static Hold getHold(String holdReqId) {
    Status holdStatus = Status.OPEN_NOT_YET_FILLED;
    int queuePosition = 2;
    String cancellationReasonId = null;
    String canceledByUserId = null;

    if (holdReqId.equals(holdCancellationHoldId)) {
      holdStatus = Status.CLOSED_CANCELED;
      queuePosition = 0;
      cancellationReasonId = holdCancellationReasonId;
      canceledByUserId = patronId;
    }

    return Hold.builder()
      .item(getItem(itemId))
      .pickupLocationId(pickupLocationId)
      .expirationDate(new Date(holdExpTs))
      .queuePosition(queuePosition)
      .requestDate(new Date(holdReqTs))
      .requestId(holdReqId)
      .status(holdStatus)
      .cancellationReasonId(cancellationReasonId)
      .canceledByUserId(canceledByUserId)
      .patronComments(patronComments)
      .build();
  }

  public static Hold getHold(String itemId, Date holdReqDate) {
    return Hold.builder()
      .item(getItem(itemId))
      .pickupLocationId(pickupLocationId)
      .expirationDate(new Date(holdExpTs))
      .queuePosition(2)
      .requestDate(holdReqDate)
      .requestId(holdReqId)
      .status(Status.OPEN_NOT_YET_FILLED)
      .patronComments(patronComments)
      .build();
  }

  public static Charge getCharge(String itemId) {
    return Charge.builder()
      .item(getItem(itemId_overdue))
      .feeFineId(feeFineId)
      .chargeAmount(new Money(1.23f, Currency.getInstance("USD").getCurrencyCode()))
      .accrualDate(new Date(accrualDateTs))
      .description("late fee")
      .state("outstanding")
      .reason("overdue item")
      .build();
  }

  public static Loan getLoan(String itemId) {
    return Loan.builder()
      .item(getItem(itemId))
      .loanDate(new Date(checkedOutTs))
      .dueDate(new Date(dueDateTs))
      .overdue(true)
      .build();
  }

  public static String getLoanJson(String patronId, String itemId) {

    String ret = null;
    try {
      Loan loan = Loan.builder()
        .item(getItem(itemId))
        .build();
      ret = loan.toJson();
    } catch (JsonProcessingException e) {
      logger.warn("Failed to generate Loan JSON", e);
    }
    return ret;
  }

  public static String getPlacedHoldJson(Hold hold) {

    String ret = null;
    try {
      ret = hold.toJson();
    } catch (JsonProcessingException e) {
      logger.warn("Failed to generate Hold JSON", e);
    }
    return ret;
  }

  public static String getRemovedHoldJson(String holdReqId) {
    String ret = null;
    try {
      Hold hold = getHold(holdReqId);
      ret = hold.toJson();
    } catch (JsonProcessingException e) {
      logger.warn("Failed to generate Hold JSON", e);
    }
    return ret;
  }

  public static String getHoldCancellation(String holdId, String canceledByUserId) {
    String ret = null;
    try {

      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
      Date canceledDate = format.parse ( "2019-11-27" );

      HoldCancellation cancellation =  HoldCancellation.builder()
        .holdId(holdId)
        .canceledDate(canceledDate)
        .cancellationReasonId(holdCancellationReasonId)
        .cancellationAdditionalInformation("I don't want it anymore")
        .canceledByUserId(canceledByUserId)
        .build();
      ret = cancellation.toJson();
    } catch (JsonProcessingException | ParseException e) {
      logger.warn("Failed to generate Hold JSON", e);
    }
    return ret;
  }

  public static String getInvalidHoldCancellation(String holdId) {
    String ret = null;
    try {

      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
      Date canceledDate = format.parse ( "2019-11-27" );

      HoldCancellation cancellation =  HoldCancellation.builder()
        .holdId(holdId)
        .canceledDate(canceledDate)
        .cancellationReasonId(null)
        .cancellationAdditionalInformation("I don't want it anymore")
        .build();
      ret = cancellation.toJson();
    } catch (JsonProcessingException | ParseException e) {
      logger.warn("Failed to generate Hold JSON", e);
    }
    return ret;
  }

  public static String readMockFile(final String path) {
    try {
      return new String(PatronMockOkapi.class.getResourceAsStream(path).readAllBytes(),
          StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
