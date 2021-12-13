package org.folio.edge.patron;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.patron.Constants.FIELD_EXPIRATION_DATE;
import static org.folio.edge.patron.Constants.FIELD_REQUEST_DATE;
import static org.folio.edge.patron.Constants.MSG_ACCESS_DENIED;
import static org.folio.edge.patron.Constants.MSG_INTERNAL_SERVER_ERROR;
import static org.folio.edge.patron.Constants.MSG_REQUEST_TIMEOUT;
import static org.folio.edge.patron.Constants.PARAM_HOLD_ID;
import static org.folio.edge.patron.Constants.PARAM_INCLUDE_CHARGES;
import static org.folio.edge.patron.Constants.PARAM_INCLUDE_HOLDS;
import static org.folio.edge.patron.Constants.PARAM_INCLUDE_LOANS;
import static org.folio.edge.patron.Constants.PARAM_INSTANCE_ID;
import static org.folio.edge.patron.Constants.PARAM_ITEM_ID;
import static org.folio.edge.patron.Constants.PARAM_LIMIT;
import static org.folio.edge.patron.Constants.PARAM_OFFSET;
import static org.folio.edge.patron.Constants.PARAM_PATRON_ID;
import static org.folio.edge.patron.Constants.PARAM_SORT_BY;
import static org.folio.edge.patron.model.HoldCancellationValidator.validateCancelHoldRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.Handler;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.edge.patron.model.error.Error;
import org.folio.edge.patron.model.error.ErrorMessage;
import org.folio.edge.patron.model.error.Errors;
import org.folio.edge.patron.utils.PatronIdHelper;
import org.folio.edge.patron.utils.PatronOkapiClient;
import org.folio.edge.patron.utils.PatronOkapiClientFactory;

public class PatronHandler extends Handler {

  public PatronHandler(SecureStore secureStore, PatronOkapiClientFactory ocf) {
    super(secureStore, ocf);
  }
  private static final Logger logger = LogManager.getLogger(Handler.class);
  private static final String CONTENT_LENGTH = "content-length";

  @Override
  protected void handleCommon(RoutingContext ctx, String[] requiredParams, String[] optionalParams,
      TwoParamVoidFunction<OkapiClient, Map<String, String>> action) {

    String extPatronId = ctx.request().getParam(PARAM_PATRON_ID);
    if (extPatronId == null || extPatronId.isEmpty()) {
      badRequest(ctx, "Missing required parameter: " + PARAM_PATRON_ID);
      return;
    }
    if (null == ctx.request().getParam(PARAM_LIMIT)) {
      ctx.request().params().add(PARAM_LIMIT, "10");
    }
    if (null == ctx.request().getParam(PARAM_OFFSET)) {
      ctx.request().params().add(PARAM_OFFSET, "0");
    }

    super.handleCommon(ctx, requiredParams, optionalParams, (client, params) -> {
      final PatronOkapiClient patronClient = new PatronOkapiClient(client);

      PatronIdHelper.lookupPatron(patronClient, client.tenant, extPatronId)
        .thenAcceptAsync(patronId -> {
          params.put(PARAM_PATRON_ID, patronId);
          action.apply(patronClient, params);
        })
        .exceptionally(t -> {
          if (t instanceof TimeoutException) {
            requestTimeout(ctx, t.getMessage());
          } else {
            notFound(ctx, "Unable to find patron " + extPatronId);
          }
          return null;
        });
    });
  }

  public void handleGetAccount(RoutingContext ctx) {
    handleCommon(ctx,
        new String[] {},
        new String[]{PARAM_INCLUDE_LOANS, PARAM_INCLUDE_CHARGES, PARAM_INCLUDE_HOLDS, PARAM_SORT_BY, PARAM_LIMIT,
        PARAM_OFFSET},
        (client, params) -> {
          boolean includeLoans = Boolean.parseBoolean(params.get(PARAM_INCLUDE_LOANS));
          boolean includeCharges = Boolean.parseBoolean(params.get(PARAM_INCLUDE_CHARGES));
          boolean includeHolds = Boolean.parseBoolean(params.get(PARAM_INCLUDE_HOLDS));
          String sortBy = params.get(PARAM_SORT_BY);
          int limit = Integer.parseInt(params.get(PARAM_LIMIT));
          int offset = Integer.parseInt(params.get(PARAM_OFFSET));

          ((PatronOkapiClient) client).getAccount(params.get(PARAM_PATRON_ID),
              includeLoans,
              includeCharges,
              includeHolds,
              sortBy,
              limit,
              offset,
              ctx.request().headers(),
              resp -> handleProxyResponse(ctx, resp),
              t -> handleProxyException(ctx, t));
        });
  }

  public void handleRenew(RoutingContext ctx) {
    handleCommon(ctx,
        new String[] { PARAM_ITEM_ID },
        new String[] {},
        (client, params) -> ((PatronOkapiClient) client).renewItem(
            params.get(PARAM_PATRON_ID),
            params.get(PARAM_ITEM_ID),
            ctx.request().headers(),
            resp -> handleProxyResponse(ctx, resp),
            t -> handleProxyException(ctx, t)));

  }

  public void handlePlaceItemHold(RoutingContext ctx) {
    final String body = checkDates(ctx.getBodyAsJson());
    handleCommon(ctx,
        new String[] { PARAM_ITEM_ID },
        new String[] {},
        (client, params) -> ((PatronOkapiClient) client).placeItemHold(
            params.get(PARAM_PATRON_ID),
            params.get(PARAM_ITEM_ID),
            body,
            ctx.request().headers().remove(CONTENT_LENGTH), //removing content-length header here as the new message's size isn't the same it was originally
            resp -> handleProxyResponse(ctx, resp),
            t -> handleProxyException(ctx, t)));
  }

  public void handleCancelHold(RoutingContext ctx) {
    String validationResult = validateCancelHoldRequest(ctx.getBodyAsJson());
    if ( validationResult != null) {
      final int errorStatusCode = 422;
      String errorMessage = get422ErrorMsg(errorStatusCode, constructValidationErrorMessage(validationResult));
      ctx.response()
        .setStatusCode(errorStatusCode)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(errorMessage);
      return;
    }

    handleCommon(ctx,
        new String[] { PARAM_PATRON_ID, PARAM_HOLD_ID },
        new String[] {},
        (client, params) ->
          ((PatronOkapiClient) client).cancelHold(
              params.get(PARAM_PATRON_ID),
              params.get(PARAM_HOLD_ID),
              ctx.getBodyAsJson(),
              ctx.request().headers().remove(CONTENT_LENGTH),
              resp -> handleProxyResponse(ctx, resp),
              t -> handleProxyException(ctx, t))
        );
  }

  public void handlePlaceInstanceHold(RoutingContext ctx) {
    final String body = checkDates(ctx.getBodyAsJson());
    handleCommon(ctx,
        new String[] { PARAM_INSTANCE_ID },
        new String[] {},
        (client, params) -> ((PatronOkapiClient) client).placeInstanceHold(
            params.get(PARAM_PATRON_ID),
            params.get(PARAM_INSTANCE_ID),
            body,
            ctx.request().headers().remove(CONTENT_LENGTH), //removing content-length header here as the new message's size isn't the same it was originally
            resp -> handleProxyResponse(ctx, resp),
            t -> handleProxyException(ctx, t)));
  }

  @Override
  protected void invalidApiKey(RoutingContext ctx, String msg) {
    accessDenied(ctx, msg);
  }

  @Override
  protected void accessDenied(RoutingContext ctx, String msg) {
    ctx.response()
      .setStatusCode(401)
      .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .end(getStructuredErrorMessage(401, MSG_ACCESS_DENIED));
  }

  @Override
  protected void badRequest(RoutingContext ctx, String msg){
    ctx.response()
      .setStatusCode(400)
      .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .end(getStructuredErrorMessage(400, msg));
  }

  @Override
  protected void notFound(RoutingContext ctx, String msg) {
    ctx.response()
      .setStatusCode(404)
      .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .end(getStructuredErrorMessage(404, msg));
  }

  @Override
  protected void requestTimeout(RoutingContext ctx, String msg) {
    ctx.response()
      .setStatusCode(408)
      .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .end(getStructuredErrorMessage(408, MSG_REQUEST_TIMEOUT));
  }

  @Override
  protected void internalServerError(RoutingContext ctx, String msg) {
    if (!ctx.response().ended()) {
      ctx.response()
        .setStatusCode(500)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(getStructuredErrorMessage(500, MSG_INTERNAL_SERVER_ERROR));
    }
  }

  @Override
  protected void handleProxyResponse(RoutingContext ctx, HttpResponse<Buffer> resp) {
    HttpServerResponse serverResponse = ctx.response();

    int statusCode = resp.statusCode();
    serverResponse.setStatusCode(statusCode);

    String respBody = resp.bodyAsString();
    if (logger.isDebugEnabled()) {
      logger.debug("response: " + respBody);
    }

    String contentType = resp.getHeader(HttpHeaders.CONTENT_TYPE.toString());

    if (resp.statusCode() < 400){
      setContentType(serverResponse, contentType);
      serverResponse.end(respBody);  //not an error case, pass on the response body as received
    }
    else {
      String errorMsg = getErrorMessage(statusCode, respBody);
      setContentType(serverResponse, APPLICATION_JSON);
      serverResponse.end(errorMsg);
    }
  }

  @Override
  protected void handleProxyException(RoutingContext ctx, Throwable t) {
    logger.error("Exception calling OKAPI", t);
    if (t instanceof TimeoutException) {
      requestTimeout(ctx, t.getMessage());
    } else {
      internalServerError(ctx, t.getMessage());
    }
  }

  private void setContentType(HttpServerResponse response, String contentType){
    if (contentType != null && !contentType.equals("")) {
        response.putHeader(HttpHeaders.CONTENT_TYPE, contentType);
    }
  }

  private static String checkDates(JsonObject requestMessage) {
    requestMessage = validateHoldsExpirationDate(requestMessage);
    return updateRequestDateWithTimestamp(requestMessage);
  }

  private static String updateRequestDateWithTimestamp(JsonObject requestMessage) {
    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    String timestamp = sdf.format(new Date());

    requestMessage.put(FIELD_REQUEST_DATE, timestamp);
    return requestMessage.encodePrettily();
  }

  private static JsonObject validateHoldsExpirationDate(JsonObject requestMessage) {
    String requestExpirationDate = requestMessage.getString(FIELD_EXPIRATION_DATE);
    try {
      if (requestExpirationDate != null && !requestExpirationDate.isEmpty()) {
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(requestExpirationDate);
      }
    } catch (Exception parseEx) {
      logger.debug("Exception parsing request expirationDate: " + requestExpirationDate);
      requestMessage.remove(FIELD_EXPIRATION_DATE);
    }
    return requestMessage;
  }

  private String getStructuredErrorMessage(int statusCode, String message){
    String finalMsg;
    try{
      ErrorMessage error = new ErrorMessage(statusCode, message);
      finalMsg = error.toJson();
    }
    catch(JsonProcessingException ex){
      finalMsg = "{ code : \"\", message : \"" + message + "\" }";
    }
    return finalMsg;
  }

  private String get422ErrorMsg(int statusCode, Errors err) {
    String errorMessage = "";
    List<Error> errors = err.getErrors();

    if (errors != null && !errors.isEmpty()) {
      Error firstErrorInstance = errors.get(0);  //get the first error message and return it.
      if (firstErrorInstance != null) {
        errorMessage = getStructuredErrorMessage(statusCode, firstErrorInstance.getMessage());
      }
    }

    if (errorMessage.equals(""))
      errorMessage = getStructuredErrorMessage(statusCode, "No error message found");

    return errorMessage;
  }

  private String get422ErrorMsg(int statusCode, String respBody){

    logger.debug("422 message: " + respBody);
    String errorMessage = "";

    try {
      Errors err = Json.decodeValue(respBody, Errors.class);
      errorMessage = get422ErrorMsg(statusCode, err);
    } catch(Exception ex) {
      logger.debug(ex.getMessage());
      errorMessage = getStructuredErrorMessage(statusCode, "A problem encountered when extracting error message");
    }
    return errorMessage;
  }

  private String getErrorMessage(int statusCode, String respBody){

    if (statusCode == 422)
      return get422ErrorMsg(statusCode, respBody);
    else
      return getStructuredErrorMessage(statusCode, respBody);
  }

  private Errors constructValidationErrorMessage(String coreMessage) {
    Error error = new Error();
    error.setCode("422");
    error.setMessage(coreMessage);

    Errors errors = new Errors();
    errors.setTotalRecords(1);
    errors.setErrors(Collections.singletonList(error));

    return errors;
  }
}
