package org.folio.edge.patron.utils;

import java.util.concurrent.CompletableFuture;

import org.apache.log4j.Logger;
import org.folio.edge.core.utils.OkapiClient;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.folio.edge.patron.model.Hold;
import org.folio.edge.patron.model.HoldCancellation;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class PatronOkapiClient extends OkapiClient {

  private static final Logger logger = Logger.getLogger(PatronOkapiClient.class);

  public PatronOkapiClient(OkapiClient client) {
    super(client);
  }

  protected PatronOkapiClient(Vertx vertx, String okapiURL, String tenant, long timeout) {
    super(vertx, okapiURL, tenant, timeout);
  }

  public void getPatron(String extPatronId, Handler<HttpClientResponse> responseHandler,
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

  public CompletableFuture<String> getPatron(String extPatronId) {
    VertxCompletableFuture<String> future = new VertxCompletableFuture<>(vertx);

    getPatron(
        extPatronId,
        resp -> resp.bodyHandler(body -> {
          int status = resp.statusCode();
          String bodyStr = body.toString();
          logger.info(String.format("Response from mod-users: (%s) body: %s", status, bodyStr));
          if (status != 200) {
            future.completeExceptionally(new PatronLookupException(bodyStr));
          } else {
            JsonObject json = body.toJsonObject();
            try {
              future.complete(json.getJsonArray("users").getJsonObject(0).getString("id"));
            } catch (Exception e) {
              logger.error("Exception parsing response from mod-users", e);
              future.completeExceptionally(new PatronLookupException(e));
            }
          }
        }),
        t -> {
          logger.error("Exception calling mod-users", t);
          future.completeExceptionally(new PatronLookupException(t));
        });
    return future;
  }

  public void getAccount(String patronId, boolean includeLoans, boolean includeCharges,
      boolean includeHolds, Handler<HttpClientResponse> responseHandler,
      Handler<Throwable> exceptionHandler) {
    getAccount(patronId, includeLoans, includeCharges, includeHolds, null, responseHandler, exceptionHandler);
  }

  public void getAccount(String patronId, boolean includeLoans, boolean includeCharges,
      boolean includeHolds, MultiMap headers, Handler<HttpClientResponse> responseHandler,
      Handler<Throwable> exceptionHandler) {
    get(
        String.format("%s/patron/account/%s?includeLoans=%s&includeCharges=%s&includeHolds=%s",
            okapiURL,
            patronId,
            includeLoans,
            includeCharges,
            includeHolds),
        tenant,
        combineHeadersWithDefaults(headers),
        responseHandler,
        exceptionHandler);
  }

  public void renewItem(String patronId, String itemId,
      Handler<HttpClientResponse> responseHandler, Handler<Throwable> exceptionHandler) {
    renewItem(patronId, itemId, null, responseHandler, exceptionHandler);
  }

  public void renewItem(String patronId, String itemId, MultiMap headers,
      Handler<HttpClientResponse> responseHandler, Handler<Throwable> exceptionHandler) {
    post(
        String.format("%s/patron/account/%s/item/%s/renew", okapiURL, patronId, itemId),
        tenant,
        null,
        combineHeadersWithDefaults(headers),
        responseHandler,
        exceptionHandler);
  }

  public void placeItemHold(String patronId, String itemId, String requestBody,
      Handler<HttpClientResponse> responseHandler, Handler<Throwable> exceptionHandler) {
    placeItemHold(patronId,
        itemId,
        requestBody,
        null,
        responseHandler,
        exceptionHandler);
  }

  public void placeItemHold(String patronId, String itemId, String requestBody, MultiMap headers,
      Handler<HttpClientResponse> responseHandler, Handler<Throwable> exceptionHandler) {
    post(
        String.format("%s/patron/account/%s/item/%s/hold", okapiURL, patronId, itemId),
        tenant,
        requestBody,
        combineHeadersWithDefaults(headers),
        responseHandler,
        exceptionHandler);
  }

  public void cancelHold(String patronId, String holdId, String requestBody,
                         Handler<HttpClientResponse> responseHandler, Handler<Throwable> exceptionHandler) {
    cancelHold(patronId,
        holdId,
        requestBody,
      null,
      responseHandler,
      exceptionHandler);
  }

  public void cancelHold(String patronId, String holdId, String requestBody, MultiMap headers,
      Handler<HttpClientResponse> responseHandler, Handler<Throwable> exceptionHandler) {

    getRequest(holdId,
      headers,
      resp -> {
        if (resp.statusCode() == 200) {
          resp.bodyHandler(body -> {
            String bodyStr = body.toString();
            try {
              HoldCancellation holdCancellationRequest = HoldCancellation.fromJson(requestBody);
              JsonObject requestToRemove = new JsonObject(bodyStr);
              Hold holdEntity = createCancellationHoldRequest(holdCancellationRequest, requestToRemove);
              post(
                String.format("%s/patron/account/%s/hold/%s/cancel", okapiURL, patronId, holdId),
                tenant,
                holdEntity.toJson(),
                combineHeadersWithDefaults(headers),
                responseHandler,
                exceptionHandler);
            } catch (Exception ex) {
              exceptionHandler.handle(ex);
            }
          });
        } else {
          responseHandler.handle(resp);
        }
      },
      exceptionHandler
    );
  }

  public void getRequest(String holdId, Handler<HttpClientResponse> responseHandler,
                      Handler<Throwable> exceptionHandler) {

    getRequest(holdId, null, responseHandler, exceptionHandler);
  }

  public void getRequest(String holdId, MultiMap headers,
                      Handler<HttpClientResponse> responseHandler, Handler<Throwable> exceptionHandler) {

    String url = String.format("%s/circulation/requests/%s", okapiURL, holdId);

    get(
      url,
      tenant,
      combineHeadersWithDefaults(headers),
      responseHandler,
      exceptionHandler);
  }

  public void placeInstanceHold(String patronId, String instanceId, String requestBody,
      Handler<HttpClientResponse> responseHandler, Handler<Throwable> exceptionHandler) {
    placeInstanceHold(patronId,
        instanceId,
        requestBody,
        null,
        responseHandler,
        exceptionHandler);
  }

  public void placeInstanceHold(String patronId, String instanceId, String requestBody, MultiMap headers,
      Handler<HttpClientResponse> responseHandler, Handler<Throwable> exceptionHandler) {
    post(
        String.format("%s/patron/account/%s/instance/%s/hold", okapiURL, patronId, instanceId),
        tenant,
        requestBody,
        combineHeadersWithDefaults(headers),
        responseHandler,
        exceptionHandler);
  }

  private Hold createCancellationHoldRequest(HoldCancellation cancellationRequest, JsonObject baseRequest) {
    return Hold.builder()
      .cancellationReasonId(cancellationRequest.cancellationReasonId)
      .canceledByUserId(cancellationRequest.canceledByUserId)
      .cancellationAdditionalInformation(cancellationRequest.cancellationAdditionalInformation)
      .canceledDate(cancellationRequest.canceledDate)
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

}
