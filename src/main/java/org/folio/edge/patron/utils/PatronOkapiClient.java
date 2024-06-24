package org.folio.edge.patron.utils;

import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.OkapiClient;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import org.folio.edge.patron.model.Hold;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;
import static org.folio.edge.patron.Constants.FIELD_CANCELED_DATE;
import static org.folio.edge.patron.Constants.FIELD_CANCELLATION_ADDITIONAL_INFO;
import static org.folio.edge.patron.Constants.FIELD_CANCELLATION_REASON_ID;

public class PatronOkapiClient extends OkapiClient {

  private static final Logger logger = LogManager.getLogger(PatronOkapiClient.class);

  public PatronOkapiClient(OkapiClient client, String alternateTenantId) {
    super(client, alternateTenantId);
  }

  private void getPatron(String extPatronId, Handler<HttpResponse<Buffer>> responseHandler,
      Handler<Throwable> exceptionHandler) {
    get(
        String.format("%s/users?query=externalSystemId==%s",
            okapiURL,
            extPatronId),
        tenant,
        defaultHeaders,
        responseHandler,
        exceptionHandler);
  }

  public Future<String> getPatron(String extPatronId) {
    Promise<String> promise = Promise.promise();

    getPatron(
        extPatronId,
        resp -> {
          int status = resp.statusCode();
          String bodyStr = resp.bodyAsString();
          logger.info(String.format("Response from mod-users: (%s) body: %s", status, bodyStr));
          if (status != 200) {
            promise.tryFail(new PatronLookupException(bodyStr));
          } else {
            JsonObject json = resp.bodyAsJsonObject();
            try {
              promise.tryComplete(json.getJsonArray("users").getJsonObject(0).getString("id"));
            } catch (Exception e) {
              logger.error("Exception parsing response from mod-users", e);
              promise.tryFail(new PatronLookupException(e));
            }
          }
        },
        t -> {
          logger.error("Exception calling mod-users", t);
          promise.tryFail(new PatronLookupException(t));
        });
    return promise.future();
  }

  public void getAccount(String patronId, boolean includeLoans, boolean includeCharges, boolean includeHolds,
      String sortBy, String limit, String offset, Handler<HttpResponse<Buffer>> responseHandler,
      Handler<Throwable> exceptionHandler) {
    String url = String.format("%s/patron/account/%s?includeLoans=%s&includeCharges=%s&includeHolds=%s",
      okapiURL,
      patronId,
      includeLoans,
      includeCharges,
      includeHolds);
    if (null != sortBy) {
      url = String.format(url + "&sortBy=%s", sortBy);
    }
    if (null != limit) {
      url = String.format(url + "&limit=%s", limit);
    }
    if (null != offset) {
      url = String.format(url + "&offset=%s", offset);
    }

    get(
        url,
        tenant,
        null,
        responseHandler,
        exceptionHandler);
  }

  public void getExtPatronAccountByEmail(String email, Handler<HttpResponse<Buffer>> responseHandler,
                                         Handler<Throwable> exceptionHandler) {
    String url = String.format("%s/patron/account/by-email/%s", okapiURL, email);
    get(
      url,
      tenant,
      null,
      responseHandler,
      exceptionHandler);
  }

  public void renewItem(String patronId, String itemId,
      Handler<HttpResponse<Buffer>> responseHandler, Handler<Throwable> exceptionHandler) {
    post(
        String.format("%s/patron/account/%s/item/%s/renew", okapiURL, patronId, itemId),
        tenant,
        null,
        null,
        responseHandler,
        exceptionHandler);
  }

  public void placeItemHold(String patronId, String itemId, String requestBody,
      Handler<HttpResponse<Buffer>> responseHandler, Handler<Throwable> exceptionHandler) {
    post(
        String.format("%s/patron/account/%s/item/%s/hold", okapiURL, patronId, itemId),
        tenant,
        requestBody,
        null,
        responseHandler,
        exceptionHandler);
  }

  public void postPatron(String requestBody,
                            Handler<HttpResponse<Buffer>> responseHandler, Handler<Throwable> exceptionHandler) {
    post(
      String.format("%s/patron/account", okapiURL),
      tenant,
      requestBody,
      null,
      responseHandler,
      exceptionHandler);
  }

  public void putPatron(String patronId, String emailId, String requestBody,
                         Handler<HttpResponse<Buffer>> responseHandler, Handler<Throwable> exceptionHandler) {
    put(
      String.format("%s/patron/account/%s/by-email/%s", okapiURL, patronId, emailId),
      tenant,
      requestBody,
      defaultHeaders,
      responseHandler,
      exceptionHandler);
  }

  public void cancelHold(String patronId, String holdId, JsonObject holdCancellationRequest,
                         Handler<HttpResponse<Buffer>> responseHandler, Handler<Throwable> exceptionHandler) {
    getRequest(holdId,
      resp -> {
        if (resp.statusCode() == 200) {
          String bodyStr = resp.bodyAsString();
          try {
            JsonObject requestToCancel = new JsonObject(bodyStr);
            Hold holdEntity = createCancellationHoldRequest(holdCancellationRequest, requestToCancel, patronId);
            post(
              String.format("%s/patron/account/%s/hold/%s/cancel", okapiURL, patronId, holdId),
              tenant,
              holdEntity.toJson(),
              null,
              responseHandler,
              exceptionHandler);
          } catch (Exception ex) {
            exceptionHandler.handle(ex);
          }
        } else {
          responseHandler.handle(resp);
        }
      },
      exceptionHandler
    );
  }

  public void getAllowedServicePoints(String patronId, String instanceId,
    Handler<HttpResponse<Buffer>> responseHandler, Handler<Throwable> exceptionHandler) {

    get(String.format("%s/patron/account/%s/instance/%s/allowed-service-points", okapiURL,
      patronId, instanceId), tenant, null, responseHandler, exceptionHandler);
  }

  public void getRequest(String holdId, Handler<HttpResponse<Buffer>> responseHandler,
                      Handler<Throwable> exceptionHandler) {

    String url = String.format("%s/circulation/requests/%s", okapiURL, holdId);

    get(
      url,
      tenant,
      null,
      responseHandler,
      exceptionHandler);
  }

  public void placeInstanceHold(String patronId, String instanceId, String requestBody,
      Handler<HttpResponse<Buffer>> responseHandler, Handler<Throwable> exceptionHandler) {
    post(
        String.format("%s/patron/account/%s/instance/%s/hold", okapiURL, patronId, instanceId),
        tenant,
        requestBody,
        null,
        responseHandler,
        exceptionHandler);
  }

  private Hold createCancellationHoldRequest(JsonObject cancellationRequest, JsonObject baseRequest, String patronId) {
    return Hold.builder()
      .cancellationReasonId(cancellationRequest.getString(FIELD_CANCELLATION_REASON_ID))
      .canceledByUserId(patronId)
      .cancellationAdditionalInformation(cancellationRequest.getString(FIELD_CANCELLATION_ADDITIONAL_INFO))
      .canceledDate(new DateTime(cancellationRequest.getString(FIELD_CANCELED_DATE), DateTimeZone.UTC).toDate())
      .requestId(baseRequest.getString("id"))
      .pickupLocationId(baseRequest.getString("pickupServicePointId"))
      .requestDate(new DateTime(baseRequest.getString("requestDate"), DateTimeZone.UTC).toDate())
      .build();
  }

  public static class PatronLookupException extends Exception {

    private static final long serialVersionUID = -8671018675309863637L;

    public PatronLookupException(Throwable t) {
      super(t);
    }

    public PatronLookupException(String msg) {
      super(msg);
    }
  }

  public void put(String url, String tenant, String payload, MultiMap headers, Handler<HttpResponse<Buffer>> responseHandler,
                  Handler<Throwable> exceptionHandler) {
    logger.debug("put:: Trying to send request to Okapi with url: {}, payload: {}, tenant: {}", url, payload, tenant);
    HttpRequest<Buffer> request = client.putAbs(url);
    if (headers != null) {
      request.headers().setAll(combineHeadersWithDefaults(headers));
    } else {
      request.headers().setAll(defaultHeaders);
    }
    logger.info("PUT '{}' tenant: {} token: {}", () -> url, () -> tenant, () -> request.headers()
      .get(X_OKAPI_TOKEN));
    request.timeout(reqTimeout);
    if (payload.isEmpty()) {
      logger.info("put:: Payload is empty");
      request.send()
        .onSuccess(responseHandler)
        .onFailure(exceptionHandler);
    } else {
      request.sendBuffer(Buffer.buffer(payload))
        .onSuccess(responseHandler)
        .onFailure(exceptionHandler);
    }
  }
}

