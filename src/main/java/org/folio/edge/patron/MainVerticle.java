package org.folio.edge.patron;

import org.folio.edge.core.EdgeVerticle;
import org.folio.edge.patron.utils.PatronOkapiClientFactory;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends EdgeVerticle {

  public MainVerticle() {
    super();
  }

  @Override
  public Router defineRoutes() {
    PatronOkapiClientFactory ocf = new PatronOkapiClientFactory(vertx, okapiURL, reqTimeoutMs);
    PatronHandler patronHandler = new PatronHandler(secureStore, ocf);

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router.route(HttpMethod.GET, "/admin/health")
      .handler(this::handleHealthCheck);

    router.route(HttpMethod.GET, "/patron/account/:patronId")
      .handler(patronHandler::handleGetAccount);

    router.route(HttpMethod.POST, "/patron/account/:patronId/item/:itemId/renew")
      .handler(patronHandler::handleRenew);

    router.route(HttpMethod.POST, "/patron/account/:patronId/item/:itemId/hold")
      .handler(patronHandler::handlePlaceItemHold);

    router.route(HttpMethod.PUT, "/patron/account/:patronId/item/:itemId/hold/:holdId")
      .handler(patronHandler::handleEditItemHold);

    router.route(HttpMethod.DELETE, "/patron/account/:patronId/item/:itemId/hold/:holdId")
      .handler(patronHandler::handleRemoveItemHold);

    router.route(HttpMethod.POST, "/patron/account/:patronId/instance/:instanceId/hold")
      .handler(patronHandler::handlePlaceInstanceHold);

    router.route(HttpMethod.PUT, "/patron/account/:patronId/instance/:instanceId/hold/:holdId")
      .handler(patronHandler::handleEditInstanceHold);

    router.route(HttpMethod.DELETE, "/patron/account/:patronId/instance/:instanceId/hold/:holdId")
      .handler(patronHandler::handleRemoveInstanceHold);
    return router;
  }
}
