package org.folio.edge.patron;

import static org.folio.edge.core.Constants.APPLICATION_JSON;
import static org.folio.edge.core.Constants.X_OKAPI_TENANT;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;
import static org.folio.edge.patron.Constants.*;
import static org.folio.edge.patron.model.HoldCancellationValidator.validateCancelHoldRequest;

import com.amazonaws.util.StringUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.Handler;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.utils.Mappers;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.edge.core.utils.OkapiClientFactory;
import org.folio.edge.patron.model.error.Error;
import org.folio.edge.patron.model.error.ErrorMessage;
import org.folio.edge.patron.model.error.Errors;
import org.folio.edge.patron.utils.KeycloakClient;
import org.folio.edge.patron.utils.KeycloakTokenHelper;
import org.folio.edge.patron.utils.PatronIdHelper;
import org.folio.edge.patron.utils.PatronOkapiClient;

public class PatronHandler extends Handler {

  public static final String WRONG_INTEGER_PARAM_MESSAGE = "'%s' parameter is incorrect."
    + " parameter value {%s} is not valid: must be an integer, greater than or equal to 0";
  private static final Logger logger = LogManager.getLogger(Handler.class);
  private final KeycloakClient keycloakClient;
  public PatronHandler(SecureStore secureStore, OkapiClientFactory ocf, KeycloakClient keycloakClient) {
    super(secureStore, ocf);
    this.keycloakClient = keycloakClient;
  }

  @Override
  protected void handleCommon(RoutingContext ctx, String[] requiredParams, String[] optionalParams,
    TwoParamVoidFunction<OkapiClient, Map<String, String>> action) {

    String extPatronId = ctx.request().getParam(PARAM_PATRON_ID);
    if (extPatronId == null || extPatronId.isEmpty()) {
      badRequest(ctx, "Missing required parameter: " + PARAM_PATRON_ID);
      return;
    }

    String offsetValue = ctx.request().getParam(PARAM_OFFSET);
    if (isRequestIntegerParamWrong(offsetValue)) {
      badRequest(ctx, String.format(String.format(WRONG_INTEGER_PARAM_MESSAGE, "offset", offsetValue), offsetValue));
      return;
    }

    String limitValue = ctx.request().getParam(PARAM_LIMIT);
    if (isRequestIntegerParamWrong(limitValue)) {
      badRequest(ctx, String.format(String.format(WRONG_INTEGER_PARAM_MESSAGE, "limit", limitValue), limitValue));
      return;
    }

    super.handleCommon(ctx, requiredParams, optionalParams, (client, params) -> {
      String alternateTenantId = ctx.request().getParam("alternateTenantId", client.tenant);
      final PatronOkapiClient patronClient = new PatronOkapiClient(client, alternateTenantId);

      PatronIdHelper.lookupPatron(patronClient, alternateTenantId, extPatronId)
        .onSuccess(patronId -> {
          params.put(PARAM_PATRON_ID, patronId);
          action.apply(patronClient, params);
        })
        .onFailure(t -> {
          logger.error("Error retrieving user data from cache or mod-user: ", t);
          if (isTimeoutException(t)) {
            requestTimeout(ctx, t.getMessage());
          } else {
            notFound(ctx, "Unable to find patron " + extPatronId);
          }
        });
    });
  }

  private void handleSecureCommon(RoutingContext ctx, Consumer<RoutingContext> handler) {
    var token = ctx.request().getHeader(X_OKAPI_TOKEN);
    var tenant = ctx.request().getHeader(X_OKAPI_TENANT);

    if (token == null || token.isEmpty()) {
      badRequest(ctx, "Missing access token");
      return;
    }

    if (tenant == null || tenant.isEmpty()) {
      badRequest(ctx, "Missing tenant id");
      return;
    }
    KeycloakTokenHelper.getClaimsFromToken(token, tenant, keycloakClient)
      .onSuccess(claims -> {
        var vip = claims.get(VIP_CLAIM, Boolean.class);
        var externalSystemId = claims.get(EXTERNAL_SYSTEM_ID_CLAIM, String.class);
        if (vip == null || externalSystemId == null) {
          logger.error("Token doesn't contain required claims");
          badRequest(ctx, "Token doesn't contain required claims");
          return;
        }
        if (!vip) {
          accessDenied(ctx, "Patron is not allowed to call secure endpoints");
          return;
        }
        ctx.request().params().add(PARAM_PATRON_ID, externalSystemId);
        handler.accept(ctx);
      })
      .onFailure(ex -> {
        logger.error("Failed to get claims from token", ex);
        badRequest(ctx, "Failed to validate access token");
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
          String limit = params.get(PARAM_LIMIT);
          String offset = params.get(PARAM_OFFSET);

          ((PatronOkapiClient) client).getAccount(params.get(PARAM_PATRON_ID),
              includeLoans,
              includeCharges,
              includeHolds,
              sortBy,
              limit,
              offset,
              resp -> handleProxyResponse(ctx, resp),
              t -> handleProxyException(ctx, t));
        });
  }

  public void handleSecureGetAccount(RoutingContext ctx) {
    handleSecureCommon(ctx, this::handleGetAccount);
  }

  public void handleRenew(RoutingContext ctx) {
    handleCommon(ctx,
        new String[] { PARAM_ITEM_ID },
        new String[] {},
        (client, params) -> ((PatronOkapiClient) client).renewItem(
            params.get(PARAM_PATRON_ID),
            params.get(PARAM_ITEM_ID),
            resp -> handleProxyResponse(ctx, resp),
            t -> handleProxyException(ctx, t)));

  }

  public void handlePlaceItemHold(RoutingContext ctx) {
    if (ctx.body().asJsonObject() == null) {
      badRequest(ctx, MSG_HOLD_NOBODY);
      return;
    }
    final String body = checkDates(ctx.body().asJsonObject());
    handleCommon(ctx,
        new String[] { PARAM_ITEM_ID },
        new String[] {},
        (client, params) -> ((PatronOkapiClient) client).placeItemHold(
            params.get(PARAM_PATRON_ID),
            params.get(PARAM_ITEM_ID),
            body,
            resp -> handleProxyResponse(ctx, resp),
            t -> handleProxyException(ctx, t)));
  }

  public void handleSecurePlaceItemHold(RoutingContext ctx) {
    handleSecureCommon(ctx, this::handlePlaceItemHold);
  }

  public void handlePatronRequest(RoutingContext ctx, BiConsumer<PatronOkapiClient, String> patronAction) {
    if (ctx.body().asJsonObject() == null) {
      logger.warn("handlePatronRequest:: missing body found");
      ctx.response()
        .setStatusCode(400)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(getErrorMsg("MISSING_BODY", "Request body must not be null"));
      return;
    }

    final String body = String.valueOf(ctx.body().asJsonObject());
    super.handleCommon(ctx, new String[]{}, new String[]{}, (client, params) -> {
      String alternateTenantId = ctx.request().getParam("alternateTenantId", client.tenant);
      final PatronOkapiClient patronClient = new PatronOkapiClient(client, alternateTenantId);
      patronAction.accept(patronClient, body);
    });
  }

  public void handlePostPatronRequest(RoutingContext ctx) {
    handlePatronRequest(ctx, (patronClient, body) ->
      patronClient.postPatron(body,
        resp -> handleProxyResponse(ctx, resp),
        t -> handleProxyException(ctx, t)));
  }

  public void handlePutPatronRequest(RoutingContext ctx) {
    handlePatronRequest(ctx, (patronClient, body) ->
      patronClient.putPatron(ctx.request().getParam(PARAM_EXTERNAL_SYSTEM_ID), body,
        resp -> handlePutPatronResponse(ctx, resp),
        t -> handleProxyException(ctx, t)));
  }


  public void handleCancelHold(RoutingContext ctx) {
    String validationResult = validateCancelHoldRequest(ctx.body().asJsonObject());
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
              ctx.body().asJsonObject(),
              resp -> handleProxyResponse(ctx, resp),
              t -> handleProxyException(ctx, t))
        );
  }

  public void handleSecureCancelHold(RoutingContext ctx) {
    handleSecureCommon(ctx, this::handleCancelHold);
  }

  public void handlePlaceInstanceHold(RoutingContext ctx) {
    if (ctx.body().asJsonObject() == null) {
      badRequest(ctx, MSG_HOLD_NOBODY);
      return;
    }
    final String body = checkDates(ctx.body().asJsonObject());
    handleCommon(ctx,
        new String[] { PARAM_INSTANCE_ID },
        new String[] {},
        (client, params) -> ((PatronOkapiClient) client).placeInstanceHold(
            params.get(PARAM_PATRON_ID),
            params.get(PARAM_INSTANCE_ID),
            body,
            resp -> handleProxyResponse(ctx, resp),
            t -> handleProxyException(ctx, t)));
  }

  public void handleSecurePlaceInstanceHold(RoutingContext ctx) {
    handleSecureCommon(ctx, this::handlePlaceInstanceHold);
  }

  public void handleGetAllowedServicePointsForInstance(RoutingContext ctx) {
    handleCommon(ctx,
      new String[] { PARAM_PATRON_ID, PARAM_INSTANCE_ID },
      new String[] {},
      (client, params) -> ((PatronOkapiClient) client).getAllowedServicePointsForInstance(
        params.get(PARAM_PATRON_ID),
        params.get(PARAM_INSTANCE_ID),
        resp -> handleProxyResponse(ctx, resp),
        t -> handleProxyException(ctx, t)));
  }

  public void handleSecureGetAllowedServicePointsForInstance(RoutingContext ctx) {
    handleSecureCommon(ctx, this::handleGetAllowedServicePointsForInstance);
  }


  public void handleGetAllowedServicePointsForItem(RoutingContext ctx) {
    handleCommon(ctx,
      new String[] { PARAM_PATRON_ID, PARAM_ITEM_ID },
      new String[] {},
      (client, params) -> ((PatronOkapiClient) client).getAllowedServicePointsForItem(
        params.get(PARAM_PATRON_ID),
        params.get(PARAM_ITEM_ID),
        resp -> handleProxyResponse(ctx, resp),
        t -> handleProxyException(ctx, t)));
  }

  public void handleSecureGetAllowedServicePointsForItem(RoutingContext ctx) {
    handleSecureCommon(ctx, this::handleGetAllowedServicePointsForItem);
  }

  public void handleGetPatronRegistrationStatus(RoutingContext ctx) {
    logger.debug("handleGetPatronRegistrationStatus:: Fetching patron registration");
    String emailId = ctx.request().getParam(PARAM_EMAIL_ID);
    if(StringUtils.isNullOrEmpty(emailId)) {
      logger.warn("handleGetPatronRegistrationStatus:: Missing or empty emailId");
      ctx.response()
        .setStatusCode(400)
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .end(getErrorMsg("EMAIL_NOT_PROVIDED", "emailId is missing in the request"));
      return;
    }
    super.handleCommon(ctx, new String[]{}, new String[]{}, (client, params) -> {
      String alternateTenantId = ctx.request().getParam("alternateTenantId", client.tenant);
      final PatronOkapiClient patronClient = new PatronOkapiClient(client, alternateTenantId);
      patronClient.getPatronRegistrationStatus(emailId,
        resp -> handleRegistrationStatusResponse(ctx, resp),
        t -> handleProxyException(ctx, t));
    });
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
    if (logger.isDebugEnabled() ) {
      logger.debug("response: " + respBody);
    }

    String contentType = resp.getHeader(HttpHeaders.CONTENT_TYPE.toString());

    if (resp.statusCode() < 400 && Objects.nonNull(respBody)){
      setContentType(serverResponse, contentType);
      serverResponse.end(respBody);  //not an error case, pass on the response body as received
    }
    else {
      String errorMsg = getErrorMessage(statusCode, respBody);
      setContentType(serverResponse, APPLICATION_JSON);
      serverResponse.end(errorMsg);
    }
  }

  protected void handleRegistrationStatusResponse(RoutingContext ctx, HttpResponse<Buffer> resp) {
    HttpServerResponse serverResponse = ctx.response();

    int statusCode = resp.statusCode();
    serverResponse.setStatusCode(statusCode);

    String respBody = resp.bodyAsString();
    if (logger.isDebugEnabled() ) {
      logger.debug("handleRegistrationStatusResponse:: response {} ", respBody);
    }

    String contentType = resp.getHeader(HttpHeaders.CONTENT_TYPE.toString());

    if (resp.statusCode() < 400 && Objects.nonNull(respBody)){
      setContentType(serverResponse, contentType);
      serverResponse.end(respBody);  //not an error case, pass on the response body as received
    }
    else {
      String errorMsg = (statusCode == 404 || statusCode == 400)
        ? getFormattedErrorMsg(statusCode, respBody)
        : getStructuredErrorMessage(statusCode, respBody);
      setContentType(serverResponse, APPLICATION_JSON);
      serverResponse.end(errorMsg);
    }
  }

  protected void handlePutPatronResponse(RoutingContext ctx, HttpResponse<Buffer> resp) {
    HttpServerResponse serverResponse = ctx.response();

    int statusCode = resp.statusCode();
    serverResponse.setStatusCode(statusCode);

    String respBody = resp.bodyAsString();
    if (logger.isDebugEnabled() ) {
      logger.debug("handlePutPatronResponse:: response {} ", respBody);
    }

    String contentType = resp.getHeader(HttpHeaders.CONTENT_TYPE.toString());

    if (resp.statusCode() < 400 && Objects.nonNull(respBody)){
      setContentType(serverResponse, contentType);
      serverResponse.end(respBody);  //not an error case, pass on the response body as received
    }
    else {
      String errorMsg = (statusCode == 404 || statusCode == 400)
        ? getFormattedErrorMsg(statusCode, respBody)
        : getErrorMessage(statusCode, respBody);
      setContentType(serverResponse, APPLICATION_JSON);
      serverResponse.end(errorMsg);
    }
  }


  @Override
  protected void handleProxyException(RoutingContext ctx, Throwable t) {
    logger.error("Exception retrieving data from mod-patron:", t);
    if (isTimeoutException(t)) {
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

  private boolean isRequestIntegerParamWrong(String paramToValidate) {
    if (paramToValidate != null) {
      try {
        int paramValue = Integer.parseInt(paramToValidate);
        if (paramValue < 0) {
          return true;
        }
      } catch (NumberFormatException nfe) {
        logger.debug("Exception during validation of query param: " + nfe.getMessage());
        return true;
      }
    }

    return false;
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

  private String getFormattedErrorMsg(int statusCode, String respBody) {
    logger.debug("getFormattedErrorMsg:: respBody {}", respBody);
    String errorMessage = "";
    try {
      var errors = Json.decodeValue(respBody, Errors.class).getErrors();
      if (errors != null && !errors.isEmpty()) {
        var error = errors.get(0);
        return getErrorMsg(error.getCode(), error.getMessage());
      }
    } catch (Exception ex) {
      logger.warn(ex.getMessage());
      errorMessage = getStructuredErrorMessage(statusCode, respBody);
    }
    return errorMessage;
  }

  private String getErrorMsg(String code, String errorMessage) {
    Map<String, String> errorMap = new HashMap<>();
    errorMap.put("errorMessage", errorMessage);
    errorMap.put("code", code);
    try {
      return Mappers.jsonMapper.writeValueAsString(errorMap);
    } catch (JsonProcessingException e) {
      return getStructuredErrorMessage(500, "A problem encountered when extracting error message");
    }
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
