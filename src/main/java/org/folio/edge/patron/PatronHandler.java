package org.folio.edge.patron;

import static org.folio.edge.core.Constants.MSG_ACCESS_DENIED;
import static org.folio.edge.core.Constants.MSG_INTERNAL_SERVER_ERROR;
import static org.folio.edge.core.Constants.MSG_REQUEST_TIMEOUT;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.patron.Constants.PARAM_HOLD_ID;
import static org.folio.edge.patron.Constants.PARAM_INCLUDE_CHARGES;
import static org.folio.edge.patron.Constants.PARAM_INCLUDE_HOLDS;
import static org.folio.edge.patron.Constants.PARAM_INCLUDE_LOANS;
import static org.folio.edge.patron.Constants.PARAM_INSTANCE_ID;
import static org.folio.edge.patron.Constants.PARAM_ITEM_ID;
import static org.folio.edge.patron.Constants.PARAM_PATRON_ID;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.folio.edge.core.Handler;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.edge.patron.utils.PatronIdHelper;
import org.folio.edge.patron.utils.PatronOkapiClient;
import org.folio.edge.patron.utils.PatronOkapiClientFactory;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

public class PatronHandler extends Handler {

  public PatronHandler(SecureStore secureStore, PatronOkapiClientFactory ocf) {
    super(secureStore, ocf);
  }

  @Override
  protected void handleCommon(RoutingContext ctx, String[] requiredParams, String[] optionalParams,
      TwoParamVoidFunction<OkapiClient, Map<String, String>> action) {

    String extPatronId = ctx.request().getParam(PARAM_PATRON_ID);
    if (extPatronId == null || extPatronId.isEmpty()) {
      badRequest(ctx, "Missing required parameter: " + PARAM_PATRON_ID);
      return;
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
        new String[] { PARAM_INCLUDE_LOANS, PARAM_INCLUDE_CHARGES, PARAM_INCLUDE_HOLDS },
        (client, params) -> {
          boolean includeLoans = Boolean.parseBoolean(params.get(PARAM_INCLUDE_LOANS));
          boolean includeCharges = Boolean.parseBoolean(params.get(PARAM_INCLUDE_CHARGES));
          boolean includeHolds = Boolean.parseBoolean(params.get(PARAM_INCLUDE_HOLDS));

          ((PatronOkapiClient) client).getAccount(params.get(PARAM_PATRON_ID),
              includeLoans,
              includeCharges,
              includeHolds,
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
    handleCommon(ctx,
        new String[] { PARAM_ITEM_ID },
        new String[] {},
        (client, params) -> ((PatronOkapiClient) client).placeItemHold(
            params.get(PARAM_PATRON_ID),
            params.get(PARAM_ITEM_ID),
            ctx.getBodyAsString(),
            ctx.request().headers(),
            resp -> handleProxyResponse(ctx, resp),
            t -> handleProxyException(ctx, t)));
  }

  public void handleEditItemHold(RoutingContext ctx) {
    handleCommon(ctx,
        new String[] { PARAM_ITEM_ID, PARAM_HOLD_ID },
        new String[] {},
        (client, params) -> ((PatronOkapiClient) client).editItemHold(
            params.get(PARAM_PATRON_ID),
            params.get(PARAM_ITEM_ID),
            params.get(PARAM_HOLD_ID),
            ctx.request().headers(),
            resp -> handleProxyResponse(ctx, resp),
            t -> handleProxyException(ctx, t)));

  }

  public void handleRemoveItemHold(RoutingContext ctx) {
    handleCommon(ctx,
        new String[] { PARAM_ITEM_ID, PARAM_HOLD_ID },
        new String[] {},
        (client, params) -> ((PatronOkapiClient) client).removeItemHold(
            params.get(PARAM_PATRON_ID),
            params.get(PARAM_ITEM_ID),
            params.get(PARAM_HOLD_ID),
            ctx.request().headers(),
            resp -> handleProxyResponse(ctx, resp),
            t -> handleProxyException(ctx, t)));
  }

  public void handlePlaceInstanceHold(RoutingContext ctx) {
    handleCommon(ctx,
        new String[] { PARAM_INSTANCE_ID },
        new String[] {},
        (client, params) -> ((PatronOkapiClient) client).placeInstanceHold(
            params.get(PARAM_PATRON_ID),
            params.get(PARAM_INSTANCE_ID),
            ctx.getBodyAsString(),
            ctx.request().headers(),
            resp -> handleProxyResponse(ctx, resp),
            t -> handleProxyException(ctx, t)));
  }

  public void handleEditInstanceHold(RoutingContext ctx) {
    handleCommon(ctx,
        new String[] { PARAM_INSTANCE_ID, PARAM_HOLD_ID },
        new String[] {},
        (client, params) -> ((PatronOkapiClient) client).editInstanceHold(
            params.get(PARAM_PATRON_ID),
            params.get(PARAM_INSTANCE_ID),
            params.get(PARAM_HOLD_ID),
            ctx.request().headers(),
            resp -> handleProxyResponse(ctx, resp),
            t -> handleProxyException(ctx, t)));
  }

  public void handleRemoveInstanceHold(RoutingContext ctx) {
    handleCommon(ctx,
        new String[] { PARAM_INSTANCE_ID, PARAM_HOLD_ID },
        new String[] {},
        (client, params) -> ((PatronOkapiClient) client).removeInstanceHold(params.get(PARAM_PATRON_ID),
            params.get(PARAM_INSTANCE_ID),
            params.get(PARAM_HOLD_ID),
            ctx.request().headers(),
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
      .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
      .end(MSG_ACCESS_DENIED);
  }

  @Override
  protected void badRequest(RoutingContext ctx, String msg) {
    ctx.response()
      .setStatusCode(400)
      .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
      .end(msg);
  }

  @Override
  protected void notFound(RoutingContext ctx, String msg) {
    ctx.response()
      .setStatusCode(404)
      .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
      .end(msg);
  }

  @Override
  protected void requestTimeout(RoutingContext ctx, String msg) {
    ctx.response()
      .setStatusCode(408)
      .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
      .end(MSG_REQUEST_TIMEOUT);
  }

  @Override
  protected void internalServerError(RoutingContext ctx, String msg) {
    if (!ctx.response().ended()) {
      ctx.response()
        .setStatusCode(500)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end(MSG_INTERNAL_SERVER_ERROR);
    }
  }
}
