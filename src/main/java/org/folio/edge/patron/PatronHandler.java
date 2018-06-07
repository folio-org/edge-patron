package org.folio.edge.patron;

import static org.folio.edge.core.Constants.PARAM_API_KEY;
import static org.folio.edge.core.Constants.TEXT_PLAIN;
import static org.folio.edge.patron.Constants.MSG_ACCESS_DENIED;
import static org.folio.edge.patron.Constants.MSG_INTERNAL_SERVER_ERROR;
import static org.folio.edge.patron.Constants.MSG_MISSING_HOLD_ID;
import static org.folio.edge.patron.Constants.MSG_MISSING_INSTANCE_ID;
import static org.folio.edge.patron.Constants.MSG_MISSING_ITEM_ID;
import static org.folio.edge.patron.Constants.MSG_REQUEST_TIMEOUT;
import static org.folio.edge.patron.Constants.PARAM_HOLD_ID;
import static org.folio.edge.patron.Constants.PARAM_INCLUDE_CHARGES;
import static org.folio.edge.patron.Constants.PARAM_INCLUDE_HOLDS;
import static org.folio.edge.patron.Constants.PARAM_INCLUDE_LOANS;
import static org.folio.edge.patron.Constants.PARAM_INSTANCE_ID;
import static org.folio.edge.patron.Constants.PARAM_ITEM_ID;
import static org.folio.edge.patron.Constants.PARAM_PATRON_ID;

import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.folio.edge.core.InstitutionalUserHelper;
import org.folio.edge.core.security.SecureStore;
import org.folio.edge.patron.utils.PatronOkapiClient;
import org.folio.edge.patron.utils.PatronOkapiClientFactory;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

public class PatronHandler {
  private static final Logger logger = Logger.getLogger(PatronHandler.class);

  private InstitutionalUserHelper iuHelper;
  private PatronOkapiClientFactory ocf;

  public PatronHandler(SecureStore secureStore, PatronOkapiClientFactory ocf) {
    this.ocf = ocf;
    this.iuHelper = new InstitutionalUserHelper(secureStore);
  }

  public void handleGetAccount(RoutingContext ctx) {
    String key = ctx.request().getParam(PARAM_API_KEY);
    String patronId = ctx.request().getParam(PARAM_PATRON_ID);

    if (key == null || key.isEmpty()) {
      accessDenied(ctx);
    } else {
      String tenant = iuHelper.getTenant(key);
      if (tenant == null) {
        accessDenied(ctx);
        return;
      }

      final PatronOkapiClient client = ocf.getPatronOkapiClient(tenant);

      iuHelper.getToken(client, tenant, tenant)
        .exceptionally(t -> {
          accessDenied(ctx);
          return null;
        })
        .thenAcceptAsync(token -> {
          client.setToken(token);

          boolean includeLoans = Boolean.parseBoolean(ctx.request().getParam(PARAM_INCLUDE_LOANS));
          boolean includeCharges = Boolean.parseBoolean(ctx.request().getParam(PARAM_INCLUDE_CHARGES));
          boolean includeHolds = Boolean.parseBoolean(ctx.request().getParam(PARAM_INCLUDE_HOLDS));

          client.getAccount(patronId, includeLoans, includeCharges, includeHolds, ctx.request().headers(),
              resp -> handleProxyResponse(ctx, resp),
              t -> handleProxyException(ctx, t));
        });
    }
  }

  public void handleRenew(RoutingContext ctx) {
    String key = ctx.request().getParam(PARAM_API_KEY);
    String patronId = ctx.request().getParam(PARAM_PATRON_ID);
    String itemId = ctx.request().getParam(PARAM_ITEM_ID);

    if (itemId == null || itemId.isEmpty()) {
      badRequest(ctx, MSG_MISSING_ITEM_ID);
    } else if (key == null || key.isEmpty()) {
      accessDenied(ctx);
    } else {
      String tenant = iuHelper.getTenant(key);
      if (tenant == null) {
        accessDenied(ctx);
        return;
      }

      final PatronOkapiClient client = ocf.getPatronOkapiClient(tenant);

      iuHelper.getToken(client, tenant, tenant)
        .exceptionally(t -> {
          accessDenied(ctx);
          return null;
        })
        .thenAcceptAsync(token -> {
          client.setToken(token);

          client.renewItem(patronId, itemId, ctx.request().headers(),
              resp -> handleProxyResponse(ctx, resp),
              t -> handleProxyException(ctx, t));
        });
    }
  }

  public void handlePlaceItemHold(RoutingContext ctx) {
    String key = ctx.request().getParam(PARAM_API_KEY);
    String patronId = ctx.request().getParam(PARAM_PATRON_ID);
    String itemId = ctx.request().getParam(PARAM_ITEM_ID);

    if (itemId == null || itemId.isEmpty()) {
      badRequest(ctx, MSG_MISSING_ITEM_ID);
    } else if (key == null || key.isEmpty()) {
      accessDenied(ctx);
    } else {
      String tenant = iuHelper.getTenant(key);
      if (tenant == null) {
        accessDenied(ctx);
        return;
      }

      final PatronOkapiClient client = ocf.getPatronOkapiClient(tenant);

      iuHelper.getToken(client, tenant, tenant)
        .exceptionally(t -> {
          accessDenied(ctx);
          return null;
        })
        .thenAcceptAsync(token -> {
          client.setToken(token);

          client.placeItemHold(patronId, itemId, ctx.getBodyAsString(), ctx.request().headers(),
              resp -> handleProxyResponse(ctx, resp),
              t -> handleProxyException(ctx, t));
        });
    }
  }

  public void handleEditItemHold(RoutingContext ctx) {
    String key = ctx.request().getParam(PARAM_API_KEY);
    String patronId = ctx.request().getParam(PARAM_PATRON_ID);
    String itemId = ctx.request().getParam(PARAM_ITEM_ID);
    String holdId = ctx.request().getParam(PARAM_HOLD_ID);

    if (itemId == null || itemId.isEmpty()) {
      badRequest(ctx, MSG_MISSING_ITEM_ID);
    } else if (holdId == null || holdId.isEmpty()) {
      badRequest(ctx, MSG_MISSING_HOLD_ID);
    } else if (key == null || key.isEmpty()) {
      accessDenied(ctx);
    } else {
      String tenant = iuHelper.getTenant(key);
      if (tenant == null) {
        accessDenied(ctx);
        return;
      }

      final PatronOkapiClient client = ocf.getPatronOkapiClient(tenant);

      iuHelper.getToken(client, tenant, tenant)
        .exceptionally(t -> {
          accessDenied(ctx);
          return null;
        })
        .thenAcceptAsync(token -> {
          client.setToken(token);

          client.editItemHold(patronId, itemId, holdId, ctx.request().headers(),
              resp -> handleProxyResponse(ctx, resp),
              t -> handleProxyException(ctx, t));
        });
    }
  }

  public void handleRemoveItemHold(RoutingContext ctx) {
    String key = ctx.request().getParam(PARAM_API_KEY);
    String patronId = ctx.request().getParam(PARAM_PATRON_ID);
    String itemId = ctx.request().getParam(PARAM_ITEM_ID);
    String holdId = ctx.request().getParam(PARAM_HOLD_ID);

    if (itemId == null || itemId.isEmpty()) {
      badRequest(ctx, MSG_MISSING_ITEM_ID);
    } else if (holdId == null || holdId.isEmpty()) {
      badRequest(ctx, MSG_MISSING_HOLD_ID);
    } else if (key == null || key.isEmpty()) {
      accessDenied(ctx);
    } else {
      String tenant = iuHelper.getTenant(key);
      if (tenant == null) {
        accessDenied(ctx);
        return;
      }

      final PatronOkapiClient client = ocf.getPatronOkapiClient(tenant);

      iuHelper.getToken(client, tenant, tenant)
        .exceptionally(t -> {
          accessDenied(ctx);
          return null;
        })
        .thenAcceptAsync(token -> {
          client.setToken(token);

          client.removeItemHold(patronId, itemId, holdId, ctx.request().headers(),
              resp -> handleProxyResponse(ctx, resp),
              t -> handleProxyException(ctx, t));
        });
    }
  }

  public void handlePlaceInstanceHold(RoutingContext ctx) {
    String key = ctx.request().getParam(PARAM_API_KEY);
    String patronId = ctx.request().getParam(PARAM_PATRON_ID);
    String itemId = ctx.request().getParam(PARAM_INSTANCE_ID);

    if (itemId == null || itemId.isEmpty()) {
      badRequest(ctx, MSG_MISSING_INSTANCE_ID);
    } else if (key == null || key.isEmpty()) {
      accessDenied(ctx);
    } else {
      String tenant = iuHelper.getTenant(key);
      if (tenant == null) {
        accessDenied(ctx);
        return;
      }

      final PatronOkapiClient client = ocf.getPatronOkapiClient(tenant);

      iuHelper.getToken(client, tenant, tenant)
        .exceptionally(t -> {
          accessDenied(ctx);
          return null;
        })
        .thenAcceptAsync(token -> {
          client.setToken(token);

          client.placeInstanceHold(patronId, itemId, ctx.getBodyAsString(), ctx.request().headers(),
              resp -> handleProxyResponse(ctx, resp),
              t -> handleProxyException(ctx, t));
        });
    }
  }

  public void handleEditInstanceHold(RoutingContext ctx) {
    String key = ctx.request().getParam(PARAM_API_KEY);
    String patronId = ctx.request().getParam(PARAM_PATRON_ID);
    String itemId = ctx.request().getParam(PARAM_INSTANCE_ID);
    String holdId = ctx.request().getParam(PARAM_HOLD_ID);

    if (itemId == null || itemId.isEmpty()) {
      badRequest(ctx, MSG_MISSING_INSTANCE_ID);
    } else if (holdId == null || holdId.isEmpty()) {
      badRequest(ctx, MSG_MISSING_HOLD_ID);
    } else if (key == null || key.isEmpty()) {
      accessDenied(ctx);
    } else {
      String tenant = iuHelper.getTenant(key);
      if (tenant == null) {
        accessDenied(ctx);
        return;
      }

      final PatronOkapiClient client = ocf.getPatronOkapiClient(tenant);

      iuHelper.getToken(client, tenant, tenant)
        .exceptionally(t -> {
          accessDenied(ctx);
          return null;
        })
        .thenAcceptAsync(token -> {
          client.setToken(token);

          client.editInstanceHold(patronId, itemId, holdId, ctx.request().headers(),
              resp -> handleProxyResponse(ctx, resp),
              t -> handleProxyException(ctx, t));
        });
    }
  }

  public void handleRemoveInstanceHold(RoutingContext ctx) {
    String key = ctx.request().getParam(PARAM_API_KEY);
    String patronId = ctx.request().getParam(PARAM_PATRON_ID);
    String itemId = ctx.request().getParam(PARAM_INSTANCE_ID);
    String holdId = ctx.request().getParam(PARAM_HOLD_ID);

    if (itemId == null || itemId.isEmpty()) {
      badRequest(ctx, MSG_MISSING_INSTANCE_ID);
    } else if (holdId == null || holdId.isEmpty()) {
      badRequest(ctx, MSG_MISSING_HOLD_ID);
    } else if (key == null || key.isEmpty()) {
      accessDenied(ctx);
    } else {
      String tenant = iuHelper.getTenant(key);
      if (tenant == null) {
        accessDenied(ctx);
        return;
      }

      final PatronOkapiClient client = ocf.getPatronOkapiClient(tenant);

      iuHelper.getToken(client, tenant, tenant)
        .exceptionally(t -> {
          accessDenied(ctx);
          return null;
        })
        .thenAcceptAsync(token -> {
          client.setToken(token);

          client.removeInstanceHold(patronId, itemId, holdId, ctx.request().headers(),
              resp -> handleProxyResponse(ctx, resp),
              t -> handleProxyException(ctx, t));
        });
    }
  }

  private void handleProxyResponse(RoutingContext ctx, HttpClientResponse resp) {
    resp.bodyHandler(body -> ctx.response()
      .setStatusCode(resp.statusCode())
      .putHeader(HttpHeaders.CONTENT_TYPE, resp.getHeader(HttpHeaders.CONTENT_TYPE))
      .end(body.toString()));
  }

  private void handleProxyException(RoutingContext ctx, Throwable t) {
    logger.error("Exception calling mod-patron (Place Instance Hold)", t);
    if (t instanceof TimeoutException) {
      requestTimeout(ctx);
    } else {
      internalServerError(ctx);
    }
  }

  private void accessDenied(RoutingContext ctx) {
    ctx.response()
      .setStatusCode(401)
      .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
      .end(MSG_ACCESS_DENIED);
  }

  private void badRequest(RoutingContext ctx, String body) {
    ctx.response()
      .setStatusCode(400)
      .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
      .end(body);
  }

  private void requestTimeout(RoutingContext ctx) {
    ctx.response()
      .setStatusCode(408)
      .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
      .end(MSG_REQUEST_TIMEOUT);
  }

  private void internalServerError(RoutingContext ctx) {
    if (!ctx.response().ended()) {
      ctx.response()
        .setStatusCode(500)
        .putHeader(HttpHeaders.CONTENT_TYPE, TEXT_PLAIN)
        .end(MSG_INTERNAL_SERVER_ERROR);
    }
  }
}
